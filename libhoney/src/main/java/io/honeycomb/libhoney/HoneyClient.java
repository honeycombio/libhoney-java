package io.honeycomb.libhoney;

import io.honeycomb.libhoney.eventdata.ResolvedEvent;
import io.honeycomb.libhoney.responses.impl.EventResponseFactory;
import io.honeycomb.libhoney.transport.Transport;
import io.honeycomb.libhoney.transport.batch.ClockProvider;
import io.honeycomb.libhoney.transport.batch.impl.SystemClockProvider;
import io.honeycomb.libhoney.transport.impl.BatchingHttpTransport;
import io.honeycomb.libhoney.utils.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <h1>The Honeycomb Client</h1>
 * This class provides functionality to construct and send events. An instance of the client is
 * immutable and threadsafe. The typical application will only have one {@link HoneyClient} instance.
 *
 * <h2>Configuration</h2>
 * Use {@link LibHoney}'s static methods to construct and configure an instance of {@link HoneyClient}.
 *
 * <h2>Usage</h2>
 * The quickest way to send an event is to use the {@link #send(Map)} method, which will send it with the settings and
 * fields of the {@link HoneyClient} instance.
 * <p>
 * However, in many cases an event should be first customised with relevant data from its runtime context.
 * {@link HoneyClient#createEvent()} creates such a customisable {@link Event}, which can then be submitted to the
 * Honeycomb server via {@link Event#send()}.
 * <p>
 * Alternatively, an {@link EventFactory} is useful when a grouping of events being sent shares common properties.
 * This can be created through the {@link HoneyClient#buildEventFactory()} method.
 *
 * <h3>Inheriting properties</h3>
 * {@link EventFactory} inherits the properties from the instance of {@link HoneyClient} that creates it.
 * An {@link Event} can be created through {@link EventFactory} and {@link HoneyClient}, and will similarly inherit
 * properties from them.
 *
 * <h3>Event Validation</h3>
 * An event being sent is subject to the following client-side validation rules, causing an IllegalArgumentException
 * to be thrown when violated:
 * <ul>
 * <li><b>apiHost</b> must not be null</li>
 * <li><b>writeKey</b> must not be blank</li>
 * <li><b>dataset</b> must not be blank</li>
 * <li><b>samplerate</b> must be 1 or greater</li>
 * <li>the event must have at least one key-value pair added to its fields</li>
 * </ul>
 * It's sufficient to configure these on any of the "scopes" (i.e. by setting them on {@link HoneyClient},
 * {@link EventFactory}, or {@link Event}), as long as as the event being sent ultimately contains them.
 *
 * <h2>Shutdown</h2>
 * To ensure a graceful shutdown (flushing of queues, shutdown of threads), make sure to call close() once this
 * instance is no longer needed.
 * After close returns, it is not longer safe to use the instance, and <b>this includes
 * any {@link EventFactory} and {@link Event} instances that have been created with this instance.</b>
 * <p>
 * The shutdown is best effort, and subject to a timeout, so it does not block indefinitely.
 * This means, events may ultimately be dropped (e.g. if the honeycomb server cannot be reached in time).
 *
 * <h2>Global Fields</h2>
 * Global fields are configured via {@link Options}. There are 2 types of fields:
 * <ul>
 * <li>{@code globalFields} have fixed keys and values and remain static for the lifetime of the instance.</li>
 * <li>{@code globalDynamicFields} also have fixed keys, but values are dynamic and resolved every time when
 * {@link Event#send()} is invoked.</li>
 * </ul>
 * Fields will be inherited by {@link EventFactory}s and {@link Event}s that are constructed from a given
 * {@link HoneyClient} instance.
 * Note that global fields have a lower precedence than fields added via {@link EventFactory} or {@link Event},
 * and thus will be overwritten if the keys equal.
 * However, across the 2 ways to provide global fields, there is no explicit precedence/ordering.
 *
 * <h2>Post Processor</h2>
 * Any global state that a user may want to observe, or any global manipulation of events, is encapsulated within the
 * post processor that a HoneyClient instance can be configured with. See the interface documentation of
 * {@link EventPostProcessor} for details.
 */
public class HoneyClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HoneyClient.class);
    private final AtomicBoolean logAdditionalPostProcessorErrors = new AtomicBoolean(true);
    private final AtomicBoolean logAdditionalDynamicFieldsErrors = new AtomicBoolean(true);
    private static final String POST_PROCESSING_ERROR_LOG_MESSAGE = "Event post-processor threw an exception, so the " +
        "event being processed could not be submitted to HoneyComb. Future post-processing errors will be logged at " +
        "DEBUG level. A ClientRejected response has been published to the ResponseObservers. Please register a " +
        "ResponseObserver to view these errors.";

    private static final String DYNAMIC_FIELDS_ERROR_LOG_MESSAGE = "Dynamic fields resolution threw an exception, so " +
        "the event being resolved could not be submitted to HoneyComb. Future dynamic field resolution errors will " +
        "be logged at DEBUG level. A ClientRejected response has been published to the ResponseObservers. " +
        "Please register a ResponseObserver to view these errors.";

    private final Transport transport;
    private final EventFactory globalEventFactory;
    private final Random sampler;
    private final EventPostProcessor postProcessor;
    private final ClockProvider clock;

    /**
     * Constructor that assumes that the default transport is being used.
     *
     * @param options Configuration options.
     */
    public HoneyClient(final Options options) {
        this(options, LibHoney.transportOptions().build());
    }

    /**
     * Constructor offering further configuration options of the transport layer.
     *
     * @param options          Configuration options.
     * @param transportOptions Advanced configuration options for the transport.
     */
    public HoneyClient(final Options options, final TransportOptions transportOptions) {
        this(options, BatchingHttpTransport.init(transportOptions), SystemClockProvider.getInstance());
        LOG.info(
            "Initialized HoneyClient with default HTTP batching transport. Basic config: {}. Transport config: {}.",
            options, transportOptions);
    }

    /**
     * Constructor that allows the the transport to be overridden. Useful for testing.
     *
     * @param options   Configuration options.
     * @param transport used to provide alternative transports (including test/mock implementations).
     */
    public HoneyClient(final Options options, final Transport transport) {
        this(options, transport, SystemClockProvider.getInstance());
    }

    /**
     * Constructor that allows the the transport and clock to be overridden. Useful for testing.
     *
     * @param options   Configuration options.
     * @param transport used to provide alternative transports (including test/mock implementations).
     * @param clock     used to override the clock for testing
     */
    public HoneyClient(final Options options, final Transport transport, final ClockProvider clock) {
        this.sampler = new Random();
        this.transport = transport;
        this.globalEventFactory = new EventFactory(this, options);
        this.postProcessor = options.getEventPostProcessor();
        this.clock = clock;
    }

    private boolean isSampled(final Event event) {
        return sampler.nextInt(event.getSampleRate()) == 0;
    }

    // Catch-all, so this doesn't tank in case of an exception. It's also part of the documented contract.
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private ResolvedEvent constructResolvedEvent(final Event event) {
        final int fieldsSize = event.getDynamicFields().size() + event.getFields().size();

        final Map<String, Object> resolvedFields = new HashMap<>(fieldsSize);

        try {
            resolveDynamicFields(event.getDynamicFields(), resolvedFields);
        } catch (final Exception e) {
            transport.getResponseObservable().publish(EventResponseFactory.dynamicFieldResolutionError(event, e));
            if (logAdditionalDynamicFieldsErrors.compareAndSet(true, false)) {
                LOG.error(DYNAMIC_FIELDS_ERROR_LOG_MESSAGE, e);
            } else {
                LOG.debug("Dynamic field resolution failed with exception", e);
            }
            return null;
        }

        // we add fields last, to make sure the map parameter to the send(Map) method overwrites other fields
        resolvedFields.putAll(event.getFields());

        final ResolvedEvent internalEvent = ResolvedEvent.of(
            resolvedFields,
            event,
            clock
        );
        if (internalEvent.getTimestamp() == null) {
            internalEvent.setTimestamp(clock.getWallTime());
        }

        // post-processing may mutate the event, so validation should happen after this
        if (postProcessor != null) {
            try {
                postProcessor.process(internalEvent);
            } catch (final Exception e) {
                transport.getResponseObservable().publish(EventResponseFactory.postProcessorError(internalEvent, e));
                if (logAdditionalPostProcessorErrors.compareAndSet(true, false)) {
                    LOG.error(POST_PROCESSING_ERROR_LOG_MESSAGE, e);
                } else {
                    LOG.debug("Dynamic field resolution failed with exception", e);
                }
                return null;
            }
        }

        // may throw IAE
        assertThatEventIsValid(internalEvent);

        return internalEvent;
    }

    /**
     * Applies basic Event Validation rules, as described on {@link HoneyClient}'s class documentation.
     *
     * @param event
     */
    private void assertThatEventIsValid(final ResolvedEvent event) {
        Assert.notNull(event.getApiHost(), "Validation failed: apiHost must not be null");
        Assert.notEmpty(event.getWriteKey(), "Validation failed: writeKey must not be null or empty");
        Assert.notEmpty(event.getDataset(), "Validation failed: dataset must not be null or empty");
        Assert.isTrue(event.getSampleRate() > 0, "Validation failed: invalid samplerate, must be greater than 1");
        Assert.notEmpty(event.getFields(),
            "Validation failed: event must have at least 1 key-value pair in its fields");
    }

    private void resolveDynamicFields(final Map<String, ValueSupplier<?>> dynamicFields,
                                      final Map<String, Object> dataMap) {
        for (final Map.Entry<String, ValueSupplier<?>> next : dynamicFields.entrySet()) {
            dataMap.put(next.getKey(), next.getValue().supply());
        }
    }

    /**
     * Sends an event, if it passes the sampling check.
     *
     * @param event to send.
     */
    void sendEvent(final Event event) {
        if (isSampled(event)) {
            sendEventPresampled(event);
        } else {
            LOG.trace("Event not sampled: {}", event);
            transport.getResponseObservable().publish(EventResponseFactory.notSampled(event));
        }
    }

    /**
     * Sends an event without sampling.
     *
     * @param event to send.
     */
    void sendEventPresampled(final Event event) {
        final ResolvedEvent resolvedEvent = constructResolvedEvent(event);
        if (resolvedEvent != null) {
            final boolean submitted = transport.submit(resolvedEvent);
            LOG.debug("Resolved event accepted onto queue: {}", resolvedEvent);
            if (!submitted) {
                LOG.debug("Resolved event rejected due to queue overflow: {}", resolvedEvent);
                transport.getResponseObservable().publish(EventResponseFactory.queueOverflow(resolvedEvent));
            }
        }
    }

    /**
     * Returns an {@link Event} with the configuration and fields populated as set during initialisation of this
     * client instance.
     *
     * @return an event.
     */
    public Event createEvent() {
        return globalEventFactory.createEvent();
    }

    /**
     * Returns a builder for constructing an {@link EventFactory}. It is initially populated with the fields and
     * configuration settings of this client instance, but can be further customised to create a more specialised
     * factory of {@link Event}s.
     *
     * @return a builder for {@link EventFactory}.
     */
    public EventFactory.Builder buildEventFactory() {
        return globalEventFactory.copy();
    }

    /**
     * Provides a shortcut to sending an event by populating it with the settings and fields of this client instance
     * and adding the provided map of fields. It is also subject to the configured sampling rate.
     *
     * @param fields to provide to the event.
     * @throws IllegalArgumentException if client-side validation fails, see {@link HoneyClient}'s class
     *                                  documentation for the "Event Validation" rules.
     */
    public void send(final Map<String, ?> fields) {
        globalEventFactory.send(fields);
    }

    /**
     * Add an observer that gets notified about the outcome of every event sent through this client.
     *
     * @param observer to register.
     */
    public void addResponseObserver(final ResponseObserver observer) {
        transport.getResponseObservable().add(observer);
    }

    /**
     * Remove the given observer, if response notifications are no longer required, or the reference
     * should be released.
     * Note that when cleaning up calling {@link #close()} will have the effect of clearing all observers.
     *
     * @param observer to remove.
     */
    public void removeResponseObserver(final ResponseObserver observer) {
        transport.getResponseObservable().remove(observer);
    }

    /**
     * Can be used to set this LibHoney instance as a global default. Shortcut for {@link LibHoney#getDefault()}.
     */
    public void setAsDefault() {
        LibHoney.setDefault(this);
    }

    /**
     * Can be used to add a JVM shutdown hook that invokes {@link #close()}  on this client instance.
     * Shortcut for {@link LibHoney#closeOnShutdown(HoneyClient)}.
     */
    public void closeOnShutdown() {
        LibHoney.closeOnShutdown(this);
    }

    /**
     * Method to initiate shutdown of this client and of the transport.
     * For details see the class documentation above.
     * After the initial call, further calls to this method have no effect.
     */
    // Catch-all, so this doesn't tank the caller in case of an exception. We log an error instead.
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    @Override
    public void close() {
        LOG.info("Close called on HoneyClient. Closing...");
        try {
            transport.close();
        } catch (final Exception e) {
            LOG.error("Closing HoneyClient's internals threw an exception", e);
        }
        LOG.info("Finished close.");
    }
}

package io.gravitee.gateway.report.impl.lmax;

import com.lmax.disruptor.EventHandler;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ClearingEventHandler implements EventHandler<ReportableEvent> {

    public void onEvent(ReportableEvent event, long sequence, boolean endOfBatch) {
        // Failing to call clear here will result in the
        // object associated with the event to live until
        // it is overwritten once the ring buffer has wrapped
        // around to the beginning.
        event.clear();
    }
}
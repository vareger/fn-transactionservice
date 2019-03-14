package com.postnode.service.bctransaction.messaging.models;

import com.postnode.service.bctransaction.database.entities.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.utils.Numeric;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionInfo {

    private byte[] transactionHash;
    private byte[] from;
    private byte[] to;
    private List<Event> events;
    private Transaction.TransactionStatus status;
    private Map<String, String> tags;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Event {
        private List<byte[]> topics;
        private byte[] data;
        private byte[] address;

        public Event(Log log) {
            topics = log.getTopics().stream().map(Numeric::hexStringToByteArray).collect(Collectors.toList());
            data = Numeric.hexStringToByteArray(log.getData());
            address = Numeric.hexStringToByteArray(log.getAddress());
        }
    }

}

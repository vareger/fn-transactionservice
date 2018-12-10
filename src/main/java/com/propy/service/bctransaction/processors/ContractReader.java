package com.propy.service.bctransaction.processors;

import com.propy.service.bctransaction.messaging.models.CallContract;
import com.propy.service.bctransaction.messaging.streams.ContractCallStreams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;

@Component
@EnableBinding(ContractCallStreams.class)
@RequiredArgsConstructor
@Slf4j
public class ContractReader {

    private final Web3j web3j;

    @StreamListener(ContractCallStreams.INPUT)
    @SendTo(ContractCallStreams.OUTPUT)
    public byte[] readContract(@Payload CallContract data) {
        try {
            EthCall call = web3j.ethCall(new Transaction(
                    Address.DEFAULT.getValue(),
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    Numeric.toHexString(data.getAddress()),
                    BigInteger.ZERO,
                    Numeric.toHexString(data.getCallData())
            ), DefaultBlockParameterName.LATEST).send();
            return Numeric.hexStringToByteArray(call.getValue());
        } catch (IOException e) {
            log.error("Contract call error", e);
        }
        return new byte[0];
    }

}

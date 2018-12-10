package com.propy.service.bctransaction.messaging.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CallContract {

    private byte[] address;
    private byte[] callData;

}

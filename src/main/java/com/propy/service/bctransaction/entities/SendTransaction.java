package com.propy.service.bctransaction.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendTransaction {

    private String sender;
    private String receiver;
    private BigInteger value;
    private byte[] data;
    private Map<String, String> tags;

}

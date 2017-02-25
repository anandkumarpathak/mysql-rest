package com.andy.security.api;

import java.math.BigInteger;
import java.security.SecureRandom;

public class HashGenerator {

    private SecureRandom secureRandom = new SecureRandom();

    public String nextHash() {
	return new BigInteger(130, secureRandom).toString(32);
    }

}

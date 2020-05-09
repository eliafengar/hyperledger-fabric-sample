package com.afengar.blockchain.hlf.counterchaincode;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.protobuf.ByteString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ResponseUtils;

public class CounterStore extends ChaincodeBase {

    private static Log _logger = LogFactory.getLog(CounterStore.class);
    private final String counterStoreKey = "Counter";

    @Override
    public Response init(ChaincodeStub stub) {
        try {
            _logger.info("Init Counter chaincode");
            List<String> args = stub.getParameters();
            if (args.size() != 1) {
                ResponseUtils.newErrorResponse("Incorrect number of arguments. Expecting 1");
            }
            // Initialize the chaincode
            int counterValue = Integer.parseInt(args.get(0));

            _logger.info(String.format("Counter Key %s, value = %s", this.counterStoreKey, counterValue));
            stub.putStringState(this.counterStoreKey, args.get(1));

            return ResponseUtils.newSuccessResponse();
        } catch (Throwable e) {
            return ResponseUtils.newErrorResponse(e);
        }
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        try {
            _logger.info("Invoke counter chaincode");
            String func = stub.getFunction();
            List<String> params = stub.getParameters();
            if (func.equals("readCounter")) {
                return readCounter(stub, params);
            }
            if (func.equals("increment")) {
                return increment(stub, params);
            }
            if (func.equals("decrement")) {
                return decrement(stub, params);
            }
            if (func.equals("reset")) {
                return reset(stub, params);
            }
            return ResponseUtils.newErrorResponse(
                    "Invalid invoke function name. Expecting one of: [\"readCounter\", \"increment\", \"decrement\", \"reset\"]");
        } catch (Throwable e) {
            return ResponseUtils.newErrorResponse(e);
        }
    }

    private Response increment(ChaincodeStub stub, List<String> args) {
        if (args.size() != 2) {
            return ResponseUtils.newErrorResponse("Incorrect number of arguments. Expecting 2");
        }
        String deltaKey = args.get(0);
        if (deltaKey.equals("delta"))
            return ResponseUtils.newErrorResponse("Missing delta argumanet");
        String deltaValue = args.get(1);
        int deltaFromValue = Integer.parseInt(deltaValue);

        String counterValue = stub.getStringState(this.counterStoreKey);
        if (counterValue == null) {
            return ResponseUtils.newErrorResponse("Counter state not found");
        }
        int counterFromValue = Integer.parseInt(counterValue);
        counterFromValue += deltaFromValue;

        _logger.info(String.format("new value of Counter: %s", counterFromValue));
        stub.putStringState(this.counterStoreKey, Integer.toString(counterFromValue));

        return ResponseUtils.newSuccessResponse("increment finished successfully", ByteString
                .copyFrom(this.counterStoreKey + ": " + counterFromValue, StandardCharsets.UTF_8).toByteArray());
    }

    private Response decrement(ChaincodeStub stub, List<String> args) {
        if (args.size() != 2) {
            return ResponseUtils.newErrorResponse("Incorrect number of arguments. Expecting 2");
        }
        String deltaKey = args.get(0);
        if (deltaKey.equals("delta"))
            return ResponseUtils.newErrorResponse("Missing delta argumanet");
        String deltaValue = args.get(1);
        int deltaFromValue = Integer.parseInt(deltaValue);

        String counterValue = stub.getStringState(this.counterStoreKey);
        if (counterValue == null) {
            return ResponseUtils.newErrorResponse("Counter state not found");
        }
        int counterFromValue = Integer.parseInt(counterValue);
        counterFromValue -= deltaFromValue;

        _logger.info(String.format("new value of Counter: %s", counterFromValue));
        stub.putStringState(this.counterStoreKey, Integer.toString(counterFromValue));

        return ResponseUtils.newSuccessResponse("decrement finished successfully", ByteString
                .copyFrom(this.counterStoreKey + ": " + counterFromValue, StandardCharsets.UTF_8).toByteArray());
    }

    // reset the key from state
    private Response reset(ChaincodeStub stub, List<String> args) {
        if (args.size() != 1) {
            return ResponseUtils.newErrorResponse("Incorrect number of arguments. Expecting 1");
        }
        String key = args.get(0);
        // Delete the key from the state in ledger
        stub.delState(key);
        return ResponseUtils.newSuccessResponse();
    }

    // readCounter callback representing the query of a chaincode
    private Response readCounter(ChaincodeStub stub, List<String> args) {
        if (args.size() != 1) {
            return ResponseUtils
                    .newErrorResponse("Incorrect number of arguments. Expecting name of the person to query");
        }
        String key = args.get(0);
        // byte[] stateBytes
        String val = stub.getStringState(key);
        if (val == null) {
            return ResponseUtils.newErrorResponse(String.format("Error: state for %s is null", key));
        }
        _logger.info(String.format("Query Response:\nName: %s, Amount: %s\n", key, val));
        return ResponseUtils.newSuccessResponse(val, ByteString.copyFrom(val, StandardCharsets.UTF_8).toByteArray());
    }

    public static void main(String[] args) {
        new CounterStore().start(args);
    }

}

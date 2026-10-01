package token;

class TokenOperation {
    String operationId;
    long amount;
    TokenState state;

    TokenOperation(String operationId, long amount, TokenState state) {
        this.operationId = operationId;
        this.amount = amount;
        this.state = state;
    }
}

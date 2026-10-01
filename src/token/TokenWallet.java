package token;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TokenWallet {
	
	private final AtomicLong availableBalance;
    private final AtomicLong reservedBalance = new AtomicLong(0);

    private final Map<String, TokenOperation> operations = new ConcurrentHashMap<>();

    public TokenWallet(long initialBalance) {
        this.availableBalance = new AtomicLong(initialBalance);
    }

    public synchronized boolean reserve(String operationId, long amount) {

        if (operations.containsKey(operationId)) {
            return true; // idempotent reserve
        }

        if (availableBalance.get() < amount) {
            return false;
        }

        availableBalance.addAndGet(-amount);
        reservedBalance.addAndGet(amount);

        operations.put(
                operationId,
                new TokenOperation(operationId, amount, TokenState.RESERVED)
        );

        return true;
    }

    public synchronized void settle(String operationId) {

        TokenOperation operation = operations.get(operationId);

        if (operation == null) {
            throw new IllegalArgumentException("Operation not found");
        }

        if (operation.state == TokenState.SETTLED) {
            return; // idempotent no-op
        }

        if (operation.state == TokenState.RELEASED) {
            return; // already refunded/released
        }

        reservedBalance.addAndGet(-operation.amount);
        operation.state = TokenState.SETTLED;
    }

    public synchronized void release(String operationId) {

        TokenOperation operation = operations.get(operationId);

        if (operation == null) {
            throw new IllegalArgumentException("Operation not found");
        }

        if (operation.state == TokenState.RELEASED) {
            return; // idempotent no-op
        }

        if (operation.state == TokenState.SETTLED) {
            return; // cannot release after final deduction
        }

        reservedBalance.addAndGet(-operation.amount);
        availableBalance.addAndGet(operation.amount);

        operation.state = TokenState.RELEASED;
    }

    public long getAvailableBalance() {
        return availableBalance.get();
    }

    public long getReservedBalance() {
        return reservedBalance.get();
    }

    public TokenState getOperationState(String operationId) {
        TokenOperation operation = operations.get(operationId);
        return operation == null ? null : operation.state;
    }
    
    public static void main(String[] args) {
		TokenWallet wallet = new TokenWallet(100);
		System.out.println("Initial Available Balance: " + wallet.getAvailableBalance()); // 100

		wallet.reserve("op-1", 40);
		System.out.println("Available Balance after reserve: " + wallet.getAvailableBalance()); // 60
		System.out.println("Reserved Balance after reserve: " + wallet.getReservedBalance()); // 40

		wallet.settle("op-1");
		System.out.println("Available Balance after settle: " + wallet.getAvailableBalance()); // 60
		System.out.println("Reserved Balance after settle: " + wallet.getReservedBalance()); // 0

		wallet.release("op-1");
		System.out.println("Available Balance after release: " + wallet.getAvailableBalance()); // 60
		System.out.println("Reserved Balance after release: " + wallet.getReservedBalance()); // 0
	}

}

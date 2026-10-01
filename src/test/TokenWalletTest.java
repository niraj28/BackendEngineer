package test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import token.TokenState;
import token.TokenWallet;

public class TokenWalletTest {
	
	  
	    void concurrentReserveShouldNotOverdrawBalance() throws Exception {

	        TokenWallet wallet = new TokenWallet(100);

	        int threadCount = 20;
	        int tokenCost = 10;

	        CountDownLatch startLatch = new CountDownLatch(1);
	        CountDownLatch doneLatch = new CountDownLatch(threadCount);

	        AtomicInteger successCount = new AtomicInteger(0);

	        for (int i = 0; i < threadCount; i++) {
	            int index = i;

	            new Thread(() -> {
	                try {
	                    startLatch.await();

	                    boolean reserved = wallet.reserve(
	                            "operation-" + index,
	                            tokenCost
	                    );

	                    if (reserved) {
	                        successCount.incrementAndGet();
	                    }

	                } catch (Exception e) {
	                    throw new RuntimeException(e);
	                } finally {
	                    doneLatch.countDown();
	                }
	            }).start();
	        }

	        startLatch.countDown();
	        doneLatch.await();

	        assertEquals(10, successCount.get());
	        assertEquals(0, wallet.getAvailableBalance());
	        assertEquals(100, wallet.getReservedBalance());
	        System.out.println("Concurrent reserve test passed.");
	    }

	    
	    private void assertEquals(int i, long availableBalance) {
			// TODO Auto-generated method stub
	    	if (i != availableBalance) {
	    		System.out.println("Expected: " + i + ", but got: " + availableBalance);
	    	}
			
		}


		private void assertEquals(int i, int j) {
			// TODO Auto-generated method stub
	    	if (i != j) {
	    		System.out.println("Expected: " + i + ", but got: " + j);
	    	}
			
		}


		void settleShouldBeIdempotent() {

	        TokenWallet wallet = new TokenWallet(100);

	        assertTrue(wallet.reserve("op-1", 40));

	        wallet.settle("op-1");
	        wallet.settle("op-1");

	        assertEquals(60, wallet.getAvailableBalance());
	        assertEquals(0, wallet.getReservedBalance());
	        assertEquals(TokenState.SETTLED, wallet.getOperationState("op-1"));
	        System.out.println("Settle idempotency test passed.");
	    }

	    
	    private void assertTrue(boolean reserve) {
			// TODO Auto-generated method stub
	    	if (!reserve) {
	    		System.out.println("Expected true, but got false");
	    	}
			
		}


		void releaseShouldBeIdempotent() {

	        TokenWallet wallet = new TokenWallet(100);

	        assertTrue(wallet.reserve("op-1", 40));

	        wallet.release("op-1");
	        wallet.release("op-1");

	        assertEquals(100, wallet.getAvailableBalance());
	        assertEquals(0, wallet.getReservedBalance());
	        assertEquals(TokenState.RELEASED, wallet.getOperationState("op-1"));
	        System.out.println("Release idempotency test passed.");
	    }


		private void assertEquals(TokenState released, TokenState operationState) {
			// TODO Auto-generated method stub
				    	if (released != operationState) {
	    		System.out.println("Expected: " + released + ", but got: " + operationState);
	    	}
		}
		
		public static void main(String[] args) throws Exception {
			TokenWalletTest test = new TokenWalletTest();
			test.concurrentReserveShouldNotOverdrawBalance();
			test.settleShouldBeIdempotent();
			test.releaseShouldBeIdempotent();
		}

}

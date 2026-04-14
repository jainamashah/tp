// ...existing code...
package seedu.crypto1010.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.KeyPair;
import seedu.crypto1010.model.Wallet;
import seedu.crypto1010.model.WalletManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;


public class SendCommandTest {

    private static final String ETH_ADDRESS = "0x1111111111111111111111111111111111111111";
    private static final String BTC_ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kygt080";
    private static final String SOL_ADDRESS = "So11111111111111111111111111111111111111112";

    private String normalizeOutput(String s) {
        return s.replaceAll("\r\n", "\n").replaceAll("[ \t]+$", "").trim();
    }

    @Test
    void execute_walletWithoutKeygen_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("noKeygen");
        SendCommand command = new SendCommand("w/noKeygen to/" + ETH_ADDRESS + " amt/1", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Must run keygen for this wallet before sending.", exception.getMessage());
    }

    @Test
    void execute_validSendWithDefaultSpeed_recordsTransactionAndHistory() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob"); // bob has balance 10 from default blockchain
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand("w/bob to/" + ETH_ADDRESS + " amt/4", walletManager);

        String output = runCommand(command, blockchain);

        String normalized = normalizeOutput(output);
        assertTrue(normalized.contains("Transaction Sent Successfully"));
        assertTrue(normalized.contains("Wallet : bob"));
        assertTrue(normalized.contains("To : " + ETH_ADDRESS));
        assertTrue(normalized.contains("Amount : 4"));
        assertTrue(normalized.contains("Speed : standard"));
        assertTrue(normalized.contains("Fee : 0.0010"));

        assertEquals(3, blockchain.size());
        assertEquals(new BigDecimal("0.999"), blockchain.getPreciseBalance("bob"));
        Wallet wallet = walletManager.findWallet("bob").orElse(null);
        assertNotNull(wallet);
        assertEquals(1, wallet.getTransactionHistory().size());
        assertTrue(wallet.getTransactionHistory().get(0).contains("speed/standard"));
    }

    @Test
    void execute_insufficientBalance_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet wallet = walletManager.createWallet("alice"); // alice has balance -10
        try {
            wallet.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand("w/alice to/" + ETH_ADDRESS + " amt/1", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Insufficient balance.", exception.getMessage());
        assertEquals(2, blockchain.size());
        assertTrue(wallet.getTransactionHistory().isEmpty());
    }

    @Test
    void execute_walletNotFound_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        SendCommand command = new SendCommand("w/nonexistent to/" + ETH_ADDRESS + " amt/1", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Wallet not found.", exception.getMessage());
    }

    @Test
    void execute_invalidAmount_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand("w/bob to/" + ETH_ADDRESS + " amt/-5", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Amount must be a positive number. Use: send w/WALLET_NAME"
            + " to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED] [fee/FEE] [note/MEMO]",
            exception.getMessage());
    }

    @Test
    void execute_extremeScientificAmount_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand("w/bob to/" + ETH_ADDRESS + " amt/1e-100000000", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Amount must be a positive number. Use: send w/WALLET_NAME"
                + " to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED] [fee/FEE] [note/MEMO]",
                exception.getMessage());
    }

    @Test
    void execute_invalidFormat_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        SendCommand command = new SendCommand("invalid", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertTrue(exception.getMessage().startsWith("Error: Invalid send format."));
    }

    @Test
    void execute_manualFeeOverride_usesProvidedFee() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand(
                "w/bob to/" + ETH_ADDRESS + " amt/4 speed/fast fee/0.5 note/priority transfer",
                walletManager);

        String output = runCommand(command, blockchain);

        String normalized = normalizeOutput(output);
        assertTrue(normalized.contains("Transaction Sent Successfully"));
        assertTrue(normalized.contains("Wallet : bob"));
        assertTrue(normalized.contains("To : " + ETH_ADDRESS));
        assertTrue(normalized.contains("Amount : 4"));
        assertTrue(normalized.contains("Speed : manual"));
        assertTrue(normalized.contains("Fee : 0.5"));
        assertTrue(normalized.contains("Note : priority transfer"));
        assertEquals(new BigDecimal("0.5"), blockchain.getPreciseBalance("network-fee"));
        Wallet wallet = walletManager.findWallet("bob").orElse(null);
        assertNotNull(wallet);
        assertTrue(wallet.getTransactionHistory().get(0).contains("note/priority transfer"));
    }

    @Test
    void execute_manualFeeOverrideWithUnsupportedSpeed_succeeds() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand(
            "w/bob to/" + ETH_ADDRESS + " amt/1 speed/ultra fee/0.1",
            walletManager);

        String output = runCommand(command, blockchain);

        String normalized = normalizeOutput(output);
        assertTrue(normalized.contains("Transaction Sent Successfully"));
        assertTrue(normalized.contains("Wallet : bob"));
        assertTrue(normalized.contains("To : " + ETH_ADDRESS));
        assertTrue(normalized.contains("Amount : 1"));
        assertTrue(normalized.contains("Speed : manual"));
        assertTrue(normalized.contains("Fee : 0.1"));
        Wallet wallet = walletManager.findWallet("bob").orElse(null);
        assertNotNull(wallet);
        assertTrue(wallet.getTransactionHistory().get(0).contains("speed/manual"));
    }

    @Test
    void execute_noteContainingPrefixLikeText_preservesEntireNote() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand(
                "w/bob to/" + ETH_ADDRESS + " amt/1 fee/0 note/repay w/alice tomorrow",
                walletManager);

        String output = runCommand(command, blockchain);

        String normalized = normalizeOutput(output);
        assertTrue(normalized.contains("Transaction Sent Successfully"));
        assertTrue(normalized.contains("Wallet : bob"));
        assertTrue(normalized.contains("To : " + ETH_ADDRESS));
        assertTrue(normalized.contains("Amount : 1"));
        assertTrue(normalized.contains("Speed : manual"));
        assertTrue(normalized.contains("Fee : 0"));
        assertTrue(normalized.contains("Note : repay w/alice tomorrow"));
        Wallet wallet = walletManager.findWallet("bob").orElse(null);
        assertNotNull(wallet);
        assertTrue(wallet.getTransactionHistory().get(0).contains("note/repay w/alice tomorrow"));
    }

    @Test
    void execute_validBitcoinAddress_succeeds() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand("w/bob to/" + BTC_ADDRESS + " amt/1 fee/0", walletManager);

        String output = runCommand(command, blockchain);

        String normalized = normalizeOutput(output);
        assertTrue(normalized.contains("Transaction Sent Successfully"));
        assertTrue(normalized.contains("Wallet : bob"));
        assertTrue(normalized.contains("To : " + BTC_ADDRESS));
        assertTrue(normalized.contains("Amount : 1"));
        assertTrue(normalized.contains("Speed : manual"));
        assertTrue(normalized.contains("Fee : 0"));
    }

    @Test
    void execute_validSolanaAddress_succeeds() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand("w/bob to/" + SOL_ADDRESS + " amt/1 fee/0", walletManager);

        String output = runCommand(command, blockchain);

        String normalized = normalizeOutput(output);
        assertTrue(normalized.contains("Transaction Sent Successfully"));
        assertTrue(normalized.contains("Wallet : bob"));
        assertTrue(normalized.contains("To : " + SOL_ADDRESS));
        assertTrue(normalized.contains("Amount : 1"));
        assertTrue(normalized.contains("Speed : manual"));
        assertTrue(normalized.contains("Fee : 0"));
    }

    @Test
    void execute_invalidAddress_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand("w/bob to/not-an-address amt/1", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Invalid recipient address. Use: send w/WALLET_NAME"
            + " to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED] [fee/FEE] [note/MEMO]",
            exception.getMessage());
    }

    @Test
    void execute_invalidBitcoinAddressCharacters_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand(
                "w/bob to/bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kygt08I amt/1",
                walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Invalid recipient address. Use: send w/WALLET_NAME"
                + " to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED] [fee/FEE] [note/MEMO]",
                exception.getMessage());
    }

    @Test
    void execute_unsupportedSpeed_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand("w/bob to/" + ETH_ADDRESS + " amt/1 speed/urgent", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Unsupported speed. Use speed/slow, speed/standard, or speed/fast."
            + " Use: send w/WALLET_NAME to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED]"
            + " [fee/FEE] [note/MEMO]", exception.getMessage());
    }

    @Test
    void execute_negativeManualFee_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        Wallet bob = walletManager.createWallet("bob");
        try {
            bob.setKeys(KeyPair.generate("btc"));
        } catch (Exception e) {
            fail("Key generation failed: " + e.getMessage());
        }
        SendCommand command = new SendCommand("w/bob to/" + ETH_ADDRESS + " amt/1 fee/-0.1", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Fee must be a non-negative number. Use: send w/WALLET_NAME"
            + " to/RECIPIENT_ADDRESS amt/AMOUNT [speed/SPEED] [fee/FEE] [note/MEMO]",
            exception.getMessage());
    }

    //    @Test
    //    void execute_sendToLocalWalletAddress_creditsReceiverBalance() throws Crypto1010Exception {
    //        Blockchain blockchain = Blockchain.createDefault();
    //        WalletManager walletManager = new WalletManager();
    //        Wallet sender = walletManager.createWallet("bob");
    //        Wallet receiver = walletManager.createWallet("carol");
    //
    //        Key[] keys = Key.generateKeyPair();
    //        receiver.setKeys(keys);
    //
    //        SendCommand command = new SendCommand(
    //                "w/bob to/" + receiver.getAddress() + " amt/2 fee/0",
    //                walletManager);
    //
    //        command.execute(blockchain);
    //
    //        assertEquals(new BigDecimal("3"), blockchain.getPreciseBalance("bob"));
    //        assertEquals(new BigDecimal("7"), blockchain.getPreciseBalance("carol"));
    //        assertEquals(new BigDecimal("0"), blockchain.getPreciseBalance(receiver.getAddress()));
    //        assertEquals(1, sender.getTransactionHistory().size());
    //    }

    private String runCommand(Command command, Blockchain blockchain) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        try {
            command.execute(blockchain);
        } catch (Crypto1010Exception e) {
            throw new RuntimeException(e);
        } finally {
            System.setOut(originalOut);
        }
        return outputStream.toString();
    }
}

// ...existing code...
package seedu.crypto1010.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Block;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.WalletManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class BalanceCommandTest {
    private String normalizeOutput(String s) {
        return s.replaceAll("\r\n", "\n").replaceAll("[ \t]+$", "").trim();
    }

    @Test
    void execute_tinyNonZeroBalance_displaysScientificNotation() throws Crypto1010Exception {
        Blockchain blockchain = new Blockchain(List.of(
            new Block(
                0,
                LocalDateTime.of(2026, 2, 12, 14, 30, 21),
                "0000000000000000",
                List.of("Genesis Block")),
            new Block(
                1,
                LocalDateTime.of(2026, 2, 12, 14, 35, 2),
                "prev-hash",
                List.of("miner -> tiny : 0.000000000000123456"))));
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("tiny");
        BalanceCommand command = new BalanceCommand("w/tiny", walletManager);

        String output = runCommand(command, blockchain);

        // Should show scientific notation for tiny non-zero balance
        boolean hasSciNotation = output.contains("e-");
        boolean notZero = !output.contains("Balance of tiny: 0.00000000");
        assertEquals(true, hasSciNotation && notZero);
    }

    @Test
    void execute_existingWallet_printsBalanceToEightDecimalPlaces() throws Crypto1010Exception {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("bob");
        BalanceCommand command = new BalanceCommand("w/bob", walletManager);

        String output = runCommand(command, blockchain);

        String expected = ""
            + "+----------------------------------------------------------------------+\n"
            + "| Wallet Balance                                                      |\n"
            + "+----------------------------------------------------------------------+\n"
            + "| Wallet : bob                                                        |\n"
            + "| Balance : 5.00000000                                                |\n"
            + "+----------------------------------------------------------------------+";
        String[] expectedLines = expected.split("\\r?\\n");
        String[] outputLines = output.split("\\r?\\n");
        assertEquals(expectedLines.length, outputLines.length, "Line count mismatch");
        for (int i = 0; i < expectedLines.length; i++) {
            String e = expectedLines[i].replaceAll("\\s+", "");
            String o = outputLines[i].replaceAll("\\s+", "");
            assertEquals(e, o, "Mismatch at line " + (i + 1));
        }
    }

    @Test
    void execute_decimalBalance_roundsToEightDecimalPlaces() throws Crypto1010Exception {
        Blockchain blockchain = new Blockchain(List.of(
                new Block(
                        0,
                        LocalDateTime.of(2026, 2, 12, 14, 30, 21),
                        "0000000000000000",
                        List.of("Genesis Block")),
                new Block(
                        1,
                        LocalDateTime.of(2026, 2, 12, 14, 35, 2),
                        "prev-hash",
                        List.of("miner -> alice : 1.234567895"))));
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("alice");
        BalanceCommand command = new BalanceCommand("w/alice", walletManager);

        String output = runCommand(command, blockchain);

        String normalized = normalizeOutput(output);
        assertTrue(normalized.contains("Wallet Balance"));
        assertTrue(normalized.contains("Wallet : alice"));
        assertTrue(normalized.contains("Balance : 1.23456790"));
    }


    @Test
    void execute_selfTransfer_keepsNetZeroBalance() throws Crypto1010Exception {
        Blockchain blockchain = new Blockchain(List.of(
            new Block(
                0,
                LocalDateTime.of(2026, 2, 12, 14, 30, 21),
                "0000000000000000",
                List.of("Genesis Block")),
            new Block(
                1,
                LocalDateTime.of(2026, 2, 12, 14, 35, 2),
                "prev-hash",
                List.of("alice -> alice : 5"))));
        WalletManager walletManager = new WalletManager();
        walletManager.createWallet("alice");
        BalanceCommand command = new BalanceCommand("w/alice", walletManager);

        String output = runCommand(command, blockchain);

        String normalized = normalizeOutput(output);
        assertTrue(normalized.contains("Wallet Balance"));
        assertTrue(normalized.contains("Wallet : alice"));
        assertTrue(normalized.contains("Balance : 0.00000000"));
    }

    @Test
    void execute_blankWalletNameAfterPrefix_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        BalanceCommand command = new BalanceCommand("w/   ", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: wallet name cannot be empty. Use: balance w/WALLET_NAME", exception.getMessage());
    }

    @Test
    void execute_missingWalletPrefix_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        BalanceCommand command = new BalanceCommand("alice", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Invalid balance format. Use: balance w/WALLET_NAME", exception.getMessage());
    }

    @Test
    void execute_walletNameWithSpaces_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        BalanceCommand command = new BalanceCommand("w/alice bob", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: wallet name must be one word without spaces. Use: balance w/WALLET_NAME",
                exception.getMessage());
    }

    @Test
    void execute_nonExistentWallet_throwsException() {
        Blockchain blockchain = Blockchain.createDefault();
        WalletManager walletManager = new WalletManager();
        BalanceCommand command = new BalanceCommand("w/ghost", walletManager);

        Crypto1010Exception exception = assertThrows(Crypto1010Exception.class, () -> command.execute(blockchain));
        assertEquals("Error: Wallet not found.", exception.getMessage());
    }

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

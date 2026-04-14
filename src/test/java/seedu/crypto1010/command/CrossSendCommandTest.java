// ...existing code...
package seedu.crypto1010.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import seedu.crypto1010.auth.AuthenticationService;
import seedu.crypto1010.exceptions.Crypto1010Exception;
import seedu.crypto1010.model.Block;
import seedu.crypto1010.model.Blockchain;
import seedu.crypto1010.model.Wallet;
import seedu.crypto1010.model.WalletManager;
import seedu.crypto1010.storage.AccountStorage;
import seedu.crypto1010.storage.BlockchainStorage;
import seedu.crypto1010.storage.WalletStorage;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CrossSendCommandTest {
    @TempDir
    Path tempDir;

    // Helper to normalize output for robust comparison
    private String normalizeOutput(String s) {
        return s.replaceAll("\r\n", "\n").replaceAll("[ \t]+$", "").trim();
    }

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("crypto1010.dataDir", tempDir.toString());
        AuthenticationService authenticationService =
            new AuthenticationService(new AccountStorage(CrossSendCommandTest.class));
        authenticationService.load();
        authenticationService.register("sender", "secret1", "secret1");
        authenticationService.register("receiver", "secret1", "secret1");
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("crypto1010.dataDir");
    }

    @Test
    void execute_validTransfer_createsRecipientWalletAndCreditsRecipient() throws Exception {
        WalletManager senderWalletManager = new WalletManager();
        Wallet senderWallet = senderWalletManager.createWallet("main", "btc");
        Blockchain senderBlockchain = blockchainWithBalance("main", "10");
        CrossSendCommand command =
            new CrossSendCommand("acc/receiver amt/2 curr/btc", senderWalletManager,
            "sender", CrossSendCommandTest.class);

        String output = runCommand(command, senderBlockchain);

        String norm = normalizeOutput(output);
        assertTrue(norm.contains("Cross-Account Transfer Completed"));
        assertTrue(norm.contains("Recipient wallet was created automatically."));
        assertTrue(norm.lines().anyMatch(line -> line.startsWith("+---")));
        assertEquals(new BigDecimal("8"), senderBlockchain.getPreciseBalance("main"));
        assertEquals(1, senderWallet.getTransactionHistory().size());
        assertEquals(
            "crossSend acc/receiver amt/2 curr/btc",
            senderWallet.getTransactionHistory().get(0)
        );

        WalletManager recipientWalletManager = new WalletStorage(CrossSendCommandTest.class, "receiver").load();
        assertEquals(1, recipientWalletManager.getWallets().size());
        Wallet recipientWallet = recipientWalletManager.getWallets().get(0);
        assertEquals("btc", recipientWallet.getName());
        assertEquals(
            "btc",
            recipientWallet.getCurrencyCode()
        );

        Blockchain recipientBlockchain =
            new BlockchainStorage(CrossSendCommandTest.class, "receiver").load();
        assertEquals(
            new BigDecimal("2"),
            recipientBlockchain.getPreciseBalance("btc")
        );
    }

    @Test
    void execute_existingRecipientCurrencyWallet_creditsExistingWallet() throws Exception {
        WalletStorage recipientWalletStorage = new WalletStorage(CrossSendCommandTest.class, "receiver");
        WalletManager recipientWalletManager = new WalletManager();
        recipientWalletManager.createWallet("vault", "btc");
        recipientWalletStorage.save(recipientWalletManager);

        WalletManager senderWalletManager = new WalletManager();
        senderWalletManager.createWallet("main", "btc");
        Blockchain senderBlockchain = blockchainWithBalance("main", "5");
        CrossSendCommand command = new CrossSendCommand(
            "acc/receiver amt/1.5 curr/btc",
            senderWalletManager,
            "sender",
            CrossSendCommandTest.class);

        String output = runCommand(command, senderBlockchain);

        String normOutput = normalizeOutput(output);
        assertTrue(normOutput.contains("Recipient wallet : vault"));
        assertTrue(normOutput.contains("Amount : 1.5"));
        assertTrue(normOutput.contains("Currency : btc"));
        assertEquals(new BigDecimal("3.5"), senderBlockchain.getPreciseBalance("main"));
        Blockchain recipientBlockchain = new BlockchainStorage(CrossSendCommandTest.class, "receiver").load();
        assertEquals(new BigDecimal("1.5"), recipientBlockchain.getPreciseBalance("vault"));
    }

    @Test
    void execute_senderCurrencyWalletMissing_throwsException() {
        WalletManager senderWalletManager = new WalletManager();
        senderWalletManager.createWallet("main");
        Blockchain senderBlockchain = blockchainWithBalance("main", "10");
        CrossSendCommand command = new CrossSendCommand(
            "acc/receiver amt/2 curr/btc",
            senderWalletManager,
            "sender",
            CrossSendCommandTest.class);

        Crypto1010Exception exception = assertThrows(
            Crypto1010Exception.class,
            () -> command.execute(senderBlockchain));

        assertEquals("Error: No wallet found for currency 'btc'.", exception.getMessage());
    }

    @Test
    void execute_recipientMissing_throwsException() {
        WalletManager senderWalletManager = new WalletManager();
        senderWalletManager.createWallet("main", "btc");
        Blockchain senderBlockchain = blockchainWithBalance("main", "10");
        CrossSendCommand command = new CrossSendCommand(
            "acc/ghost amt/2 curr/btc",
            senderWalletManager,
            "sender",
            CrossSendCommandTest.class);

        Crypto1010Exception exception = assertThrows(
            Crypto1010Exception.class,
            () -> command.execute(senderBlockchain));

        assertEquals("Error: Recipient account not found.", exception.getMessage());
    }

    @Test
    void execute_insufficientBalance_throwsException() {
        WalletManager senderWalletManager = new WalletManager();
        senderWalletManager.createWallet("main", "btc");
        Blockchain senderBlockchain = blockchainWithBalance("main", "1");
        CrossSendCommand command = new CrossSendCommand(
            "acc/receiver amt/2 curr/btc",
            senderWalletManager,
            "sender",
            CrossSendCommandTest.class);

        Crypto1010Exception exception = assertThrows(
            Crypto1010Exception.class,
            () -> command.execute(senderBlockchain));

        assertEquals("Error: Insufficient balance.", exception.getMessage());
    }

    @Test
    void execute_sameAccount_throwsException() {
        WalletManager senderWalletManager = new WalletManager();
        senderWalletManager.createWallet("main", "btc");
        Blockchain senderBlockchain = blockchainWithBalance("main", "10");
        CrossSendCommand command = new CrossSendCommand(
            "acc/sender amt/2 curr/btc",
            senderWalletManager,
            "sender",
            CrossSendCommandTest.class);

        Crypto1010Exception exception = assertThrows(
            Crypto1010Exception.class,
            () -> command.execute(senderBlockchain));

        assertEquals("Error: Cannot send to your own account.", exception.getMessage());
    }


    @Test
    void execute_invalidCurrency_throwsException() {
        WalletManager senderWalletManager = new WalletManager();
        senderWalletManager.createWallet("main", "btc");
        Blockchain senderBlockchain = blockchainWithBalance("main", "10");
        CrossSendCommand command =
            new CrossSendCommand("acc/receiver amt/2 curr/b", senderWalletManager,
                "sender", CrossSendCommandTest.class);

        Crypto1010Exception exception = assertThrows(
            Crypto1010Exception.class,
            () -> command.execute(senderBlockchain));

        assertEquals("Error: CURRENCY must be 2-10 alphanumeric characters. "
            + "Use: crossSend acc/ACCOUNT_NAME amt/AMOUNT curr/CURRENCY", exception.getMessage());
    }

    @Test
    void execute_extremeScientificAmount_throwsException() {
        WalletManager senderWalletManager = new WalletManager();
        senderWalletManager.createWallet("main", "btc");
        Blockchain senderBlockchain = blockchainWithBalance("main", "10");
        CrossSendCommand command = new CrossSendCommand(
            "acc/receiver amt/1e-100000000 curr/btc",
            senderWalletManager,
            "sender",
            CrossSendCommandTest.class);

        Crypto1010Exception exception = assertThrows(
            Crypto1010Exception.class,
            () -> command.execute(senderBlockchain));

        assertEquals("Error: Amount must be a positive number. "
            + "Use: crossSend acc/ACCOUNT_NAME amt/AMOUNT curr/CURRENCY", exception.getMessage());
    }

    private Blockchain blockchainWithBalance(String walletName, String amount) {
        Block genesis = new Block(
            0,
            LocalDateTime.of(2026, 2, 12, 14, 30, 21),
            "0000000000000000",
            List.of("Genesis Block"));
        Block fundedBlock = new Block(
            1,
            LocalDateTime.of(2026, 2, 12, 14, 35, 2),
            genesis.getCurrentHash(),
            List.of("network -> " + walletName + " : " + amount));
        return new Blockchain(List.of(genesis, fundedBlock));
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

package t_12.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import t_12.backend.entity.Block;
import t_12.backend.entity.BlockTransaction;
import t_12.backend.entity.Listing;
import t_12.backend.entity.StakingEvent;
import t_12.backend.entity.Transaction;
import t_12.backend.entity.Validator;
import t_12.backend.entity.Wallet;
import t_12.backend.repository.BlockRepository;
import t_12.backend.repository.BlockTransactionRepository;
import t_12.backend.repository.ListingRepository;
import t_12.backend.repository.StakingEventRepository;
import t_12.backend.repository.TransactionRepository;
import t_12.backend.repository.ValidatorRepository;
import t_12.backend.repository.WalletRepository;

/**
 * Verifies that the blockchain schema and repository query methods work end to
 * end.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:schema_integration_test_db;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class SchemaIntegrationTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private BlockTransactionRepository blockTransactionRepository;

    @Autowired
    private ValidatorRepository validatorRepository;

    @Autowired
    private StakingEventRepository stakingEventRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Test
    void repositoriesCanPersistAndQueryBlockchainTables() {
        Wallet wallet = new Wallet();
        wallet.setUserId(99);
        wallet.setWalletAddress("wlt_test_address");
        wallet.setPublicKey("pub_test_key");
        wallet.setCoinBalance(BigDecimal.ZERO);
        wallet.setCreatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        assertTrue(walletRepository.findByWalletAddress("wlt_test_address").isPresent());
        assertTrue(walletRepository.findByPublicKey("pub_test_key").isPresent());

        Block block = new Block();
        block.setBlockHeight(1000);
        block.setPreviousHash("prev_hash");
        block.setBlockHash("block_hash_1000");
        block.setValidatorAddress("wlt_test_address");
        block.setTimestamp(LocalDateTime.now());
        block.setTransactionCount(1);
        block.setStatus(Block.Status.COMMITTED);
        block = blockRepository.save(block);

        Transaction tx = new Transaction();
        tx.setSenderAddress("wlt_test_address");
        tx.setReceiverAddress("wlt_receiver");
        tx.setAmount(new BigDecimal("1.50000000"));
        tx.setFee(new BigDecimal("0.01000000"));
        tx.setNonce(1);
        tx.setTimestamp(LocalDateTime.now());
        tx.setTransactionHash("tx_hash_1");
        tx.setStatus(Transaction.Status.CONFIRMED);
        tx.setBlockId(block.getId());
        tx = transactionRepository.save(tx);

        BlockTransaction bt = new BlockTransaction();
        bt.setBlockId(block.getId());
        bt.setTransactionId(tx.getId());
        blockTransactionRepository.save(bt);

        Validator validator = new Validator();
        validator.setWalletAddress("wlt_test_address");
        validator.setStakedAmount(new BigDecimal("250.00000000"));
        validator.setStatus(Validator.Status.ACTIVE);
        validator.setJoinedAt(LocalDateTime.now());
        validatorRepository.save(validator);

        StakingEvent stakingEvent = new StakingEvent();
        stakingEvent.setWalletAddress("wlt_test_address");
        stakingEvent.setEventType(StakingEvent.EventType.STAKE);
        stakingEvent.setAmount(new BigDecimal("25.00000000"));
        stakingEvent.setCreatedAt(LocalDateTime.now());
        stakingEventRepository.save(stakingEvent);

        Listing listing = new Listing();
        listing.setSellerId(99);
        listing.setTitle("Validator Node Setup");
        listing.setDescription("Service listing");
        listing.setPrice(new BigDecimal("9.99000000"));
        listing.setCategory("Services");
        listing.setStatus(Listing.Status.ACTIVE);
        listing.setCreatedAt(LocalDateTime.now());
        listingRepository.save(listing);

        assertTrue(transactionRepository.findByTransactionHash("tx_hash_1").isPresent());
        assertEquals(1, transactionRepository.findByStatus(Transaction.Status.CONFIRMED).size());
        assertEquals(1, transactionRepository.findBySenderAddress("wlt_test_address").size());

        assertTrue(blockRepository.findByBlockHeight(1000).isPresent());
        assertTrue(blockRepository.findByStatus(Block.Status.COMMITTED).size() >= 1);
        assertEquals(1, blockRepository.findByValidatorAddress("wlt_test_address").size());

        assertEquals(1, blockTransactionRepository.findByBlockId(block.getId()).size());
        assertEquals(1, blockTransactionRepository.findByTransactionId(tx.getId()).size());

        assertTrue(validatorRepository.findByWalletAddress("wlt_test_address").isPresent());
        assertEquals(1, validatorRepository.findByStatus(Validator.Status.ACTIVE).size());

        assertEquals(1, stakingEventRepository.findByWalletAddress("wlt_test_address").size());
        assertEquals(1, stakingEventRepository.findByEventType(StakingEvent.EventType.STAKE).size());

        assertEquals(1, listingRepository.findByStatus(Listing.Status.ACTIVE).size());
        assertEquals(1, listingRepository.findBySellerId(99).size());
    }
}

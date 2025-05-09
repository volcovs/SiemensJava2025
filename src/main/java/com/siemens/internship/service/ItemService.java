package com.siemens.internship.service;

import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    // Thread pool for asynchronous task execution
    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    // Thread-safe list to store processed items
    private List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());
    // Atomic counter for processed item count (thread-safe)
    private AtomicInteger processedCount = new AtomicInteger(0);

    // Fetch all items from database
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    // Fetch a single item from database by its id
    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    // Save or update an item in the database
    public Item save(Item item) {
        return itemRepository.save(item);
    }

    // Delete an item by its id
    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }

    // Delete all items - clean up the database
    public void deleteAll() {
        itemRepository.deleteAll();
    }

    /**
     * Processes all items asynchronously:
     * - Uses CompletableFuture and a thread pool
     * - Waits for all tasks to complete before returning result
     * - Ensures thread safety and proper error handling
     * - Stores the processedItems in a thread-safe list
     * - Increments an atomic counter whenever an item is processed succesfully
     */
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Void>> futures = itemIds.stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(100);

                        Optional<Item> optionalItem = itemRepository.findById(id);
                        if (optionalItem.isPresent()) {
                            Item item = optionalItem.get();
                            item.setStatus("PROCESSED");
                            itemRepository.save(item);
                            processedItems.add(item);
                            processedCount.getAndIncrement();
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Interrupted while processing item ID " + id);
                    } catch (Exception ex) {
                        System.err.println("Failed to process item ID " + id + ": " + ex.getMessage());
                    }
                }, executor))
                .toList();

        return CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> processedItems);
    }
}


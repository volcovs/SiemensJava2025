package com.siemens.internship.controller;

import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/items")
@Validated
public class ItemController {
    @Autowired
    private ItemService itemService;

    // GET retrieve all items from the database
    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        List<Item> allItems = itemService.findAll();

        if (allItems.isEmpty()) {
            return new ResponseEntity<>(allItems, HttpStatus.NO_CONTENT);
        }
        else {
            return new ResponseEntity<>(allItems, HttpStatus.OK);
        }
    }

    // POST create a new item with validation for the email field
    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody Item item) {
        return new ResponseEntity<>(itemService.save(item), HttpStatus.CREATED);
    }

    // GET retrieve an item by its id (returns code 404 if the item couldn't be found)
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        Optional<Item> existingItem = itemService.findById(id);
        return existingItem.map(item -> new ResponseEntity<>(item, HttpStatus.OK)).orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // PUT update an existing item (returns code 404 if the item couldn't be found)
    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @Valid @RequestBody Item item) {
        Optional<Item> existingItem = itemService.findById(id);
        if (existingItem.isPresent()) {
            item.setId(id);
            return new ResponseEntity<>(itemService.save(item), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // DELETE item by its id (returns code 404 if the item couldn't be found)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        Optional<Item> existingItem = itemService.findById(id);
        if (existingItem.isPresent()) {
            itemService.deleteById(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // GET trigger asynchronous item processing for all items
    @GetMapping("/process")
    public CompletableFuture<ResponseEntity<List<Item>>> processItems() {
        return itemService.processItemsAsync()
                .thenApply(items -> new ResponseEntity<>(items, HttpStatus.OK));
    }
}

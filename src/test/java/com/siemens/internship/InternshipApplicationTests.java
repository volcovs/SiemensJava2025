package com.siemens.internship;

import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InternshipApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ItemService itemService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void clearDatabase() {
		itemService.deleteAll();
	}

	@Test
	void contextLoads() {
		// Mock test to see that everything is set up
	}

	@Test
	void shouldCreateAndRetrieveThreeItems() throws Exception {
		for (int i = 1; i <= 3; i++) {
			mockMvc.perform(post("/api/items")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
                    {
                      "name": "Item %d",
                      "description": "Test",
                      "status": "NOT PROCESSED",
                      "email": "test%d@example.com"
                    }
                    """.formatted(i, i)))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	void shouldHandleValidAndInvalidItems() throws Exception {
		// Valid
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                {
                  "name": "Valid Item",
                  "description": "Test",
                  "status": "OK",
                  "email": "valid@example.com"
                }
            """))
				.andExpect(status().isCreated());

		// Invalid email
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                {
                  "name": "Invalid Item",
                  "description": "Test",
                  "status": "OK",
                  "email": "invalid-email"
                }
            """))
				.andExpect(status().isBadRequest());

		// Invalid email
		mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                {
                  "name": "Invalid Item",
                  "description": "Test",
                  "status": "OK",
                  "email": "mail.com"
                }
            """))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldUpdateItemSuccessfully() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                {
                  "name": "Old Item",
                  "description": "Old",
                  "status": "NOT PROCESSED",
                  "email": "update@example.com"
                }
            """))
				.andExpect(status().isCreated())
				.andReturn();

		Item created = objectMapper.readValue(result.getResponse().getContentAsString(), Item.class);

		mockMvc.perform(put("/api/items/" + created.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                {
                  "name": "Updated Item",
                  "description": "Updated",
                  "status": "UPDATED",
                  "email": "update@example.com"
                }
            """))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/items/" + created.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Item"));
	}

	@Test
	void shouldFailToUpdateWithInvalidEmail() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                {
                  "name": "To Update",
                  "description": "Test",
                  "status": "NOT PROCESSED",
                  "email": "ok@example.com"
                }
            """))
				.andExpect(status().isCreated())
				.andReturn();

		Item item = objectMapper.readValue(result.getResponse().getContentAsString(), Item.class);

		mockMvc.perform(put("/api/items/" + item.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                {
                  "name": "Broken",
                  "description": "Invalid update",
                  "status": "ERROR",
                  "email": "notanemail"
                }
            """))
				.andExpect(status().isBadRequest());
	}

	@Test
	void shouldFindByIdAndHandleNotFound() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                {
                  "name": "Find Me",
                  "description": "Searchable",
                  "status": "OK",
                  "email": "findme@example.com"
                }
            """))
				.andExpect(status().isCreated())
				.andReturn();

		Item item = objectMapper.readValue(result.getResponse().getContentAsString(), Item.class);

		mockMvc.perform(get("/api/items/" + item.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Find Me"));

		mockMvc.perform(get("/api/items/99999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldDeleteThreeItems() throws Exception {
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(post("/api/items")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
                    {
                      "name": "DeleteMe%d",
                      "description": "Remove",
                      "status": "OK",
                      "email": "delete%d@example.com"
                    }
                """.formatted(i, i)))
					.andExpect(status().isCreated());
		}

		List<Item> items = itemService.findAll();
		for (Item item : items) {
			mockMvc.perform(delete("/api/items/" + item.getId()))
					.andExpect(status().isOk());
		}

		mockMvc.perform(get("/api/items"))
				.andExpect(status().isNoContent());
	}

	@Test
	void shouldHandleDeleteErrors() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
                {
                  "name": "Will be deleted",
                  "description": "Temp",
                  "status": "OK",
                  "email": "del@example.com"
                }
            """))
				.andExpect(status().isCreated())
				.andReturn();

		Item item = objectMapper.readValue(result.getResponse().getContentAsString(), Item.class);

		mockMvc.perform(delete("/api/items/" + item.getId()))
				.andExpect(status().isOk());

		mockMvc.perform(delete("/api/items/123456"))
				.andExpect(status().isNotFound());
	}

	@Test
	void shouldProcessItemsAndUpdateStatus() throws Exception {
		for (int i = 1; i <= 5; i++) {
			mockMvc.perform(post("/api/items")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
                    {
                      "name": "Item%d",
                      "description": "To Process",
                      "status": "NOT PROCESSED",
                      "email": "process%d@example.com"
                    }
                """.formatted(i, i)))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(get("/api/items/process"))
				.andExpect(status().isOk());

		// Wait a moment for async process to finish
		Thread.sleep(1000);

		List<Item> processed = itemService.findAll();
		for (Item item : processed) {
			assertEquals("PROCESSED", item.getStatus());
		}
	}



}

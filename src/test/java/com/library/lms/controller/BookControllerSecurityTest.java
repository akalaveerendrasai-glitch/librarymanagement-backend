package com.library.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.lms.config.SecurityConfig;
import com.library.lms.dto.request.BookRequestDTO;
import com.library.lms.dto.response.BookResponseDTO;
import com.library.lms.dto.response.PagedResponse;
import com.library.lms.security.CustomAccessDeniedHandler;
import com.library.lms.security.CustomUserDetailsService;
import com.library.lms.security.JwtAuthenticationEntryPoint;
import com.library.lms.security.JwtAuthenticationFilter;
import com.library.lms.security.JwtTokenProvider;
import com.library.lms.service.BookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class BookControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Public access: GET /api/books should return 200 OK without authentication")
    void testPublicUserCanListBooks_200Ok() throws Exception {
        PagedResponse<BookResponseDTO> emptyPage = new PagedResponse<>(Collections.emptyList(), 0, 10, 0, 0, true);
        when(bookService.getAllBooks(anyInt(), anyInt(), anyString(), anyString())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Security: POST /api/books without token should return 401 Unauthorized")
    void testUnauthenticatedUserCannotCreateBook_401Unauthorized() throws Exception {
        BookRequestDTO request = new BookRequestDTO("Book", "Author", "ISBN123", "Cat", 5, 5, "Desc");

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = {"USER"})
    @DisplayName("Role RBAC: POST /api/books with ROLE_USER should return 403 Forbidden")
    void testStudentUserCannotCreateBook_403Forbidden() throws Exception {
        BookRequestDTO request = new BookRequestDTO("Book", "Author", "ISBN123", "Cat", 5, 5, "Desc");

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = {"ADMIN"})
    @DisplayName("Role RBAC: POST /api/books with ROLE_ADMIN should return 201 Created")
    void testAdminUserCanCreateBook_201Created() throws Exception {
        BookRequestDTO request = new BookRequestDTO("Clean Architecture", "Robert Martin", "9780134494166", "Architecture", 5, 5, "Desc");
        BookResponseDTO response = new BookResponseDTO(1L, "Clean Architecture", "Robert Martin", "9780134494166", "Architecture", 5, 5, "Desc", null);

        when(bookService.createBook(any(BookRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "student@example.com", roles = {"USER"})
    @DisplayName("Role RBAC: DELETE /api/books/1 with ROLE_USER should return 403 Forbidden")
    void testStudentUserCannotDeleteBook_403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = {"ADMIN"})
    @DisplayName("Role RBAC: DELETE /api/books/1 with ROLE_ADMIN should return 204 No Content")
    void testAdminUserCanDeleteBook_204NoContent() throws Exception {
        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());
    }
}

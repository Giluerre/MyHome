package com.myhome.services.integration;

import com.myhome.controllers.integration.ControllerIntegrationTestBase;
import com.myhome.controllers.request.CreateUserRequest;
import com.myhome.controllers.request.LoginUserRequest;
import com.myhome.domain.HouseMember;
import com.myhome.domain.HouseMemberDocument;
import com.myhome.repositories.HouseMemberDocumentRepository;
import com.myhome.repositories.HouseMemberRepository;
import com.myhome.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "files.maxSizeKBytes=1",
    "files.compressionBorderSizeKBytes=99",
    "files.compressedImageQuality=0.99"
})
public class HouseMemberDocumentServiceIntegrationTest extends ControllerIntegrationTestBase {

  private static final String MEMBER_ID = "default-member-id-for-testing";
  private static final String TEST_DOCUMENT_NAME = "test-document";
  private static final String testUserName = "Test User";
  private static final String testUserEmail = "testuser@myhome.com";
  private static final String testUserPassword = "testpassword";
  @Autowired
  private HouseMemberDocumentRepository houseMemberDocumentRepository;
  @Autowired
  private HouseMemberRepository houseMemberRepository;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  void authUser() {
    if(!houseMemberRepository.findByMemberId(MEMBER_ID).isPresent()) {
      HouseMember member = houseMemberRepository.save(new HouseMember(MEMBER_ID, null, "test-member-name", null));
      member.setMemberId(MEMBER_ID);
      houseMemberRepository.save(member);
    }
    authDefaultUser();
  }

  @AfterEach()
  void cleanHouseMemberDocument() {
    HouseMember member = houseMemberRepository.findByMemberId(MEMBER_ID).get();
    member.setHouseMemberDocument(null);
    houseMemberRepository.save(member);
  }

  @Test
  void getHouseMemberDocumentSuccess() throws Exception {
    addDefaultHouseMemberDocument();

    MvcResult mvcResult = mockMvc.perform(get("/members/{memberId}/documents", MEMBER_ID)
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn();
    HouseMember member = houseMemberRepository.findByMemberId(MEMBER_ID).get();

    assertEquals(mvcResult.getResponse().getContentType(), MediaType.IMAGE_JPEG_VALUE);
    assertEquals(mvcResult.getResponse().getContentAsByteArray().length, member.getHouseMemberDocument().getDocumentContent().length);
  }

  @Test
  void getHouseMemberDocumentMemberNotExists() throws Exception {
    addDefaultHouseMemberDocument();

    mockMvc.perform(get("/members/{memberId}/documents", "non-existing-member-id")
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  void addHouseMemberDocumentSuccess() throws Exception {
    byte[] imageBytes = getImageAsByteArray(10, 10);
    MockMultipartFile mockImageFile = new MockMultipartFile("memberDocument", imageBytes);

    mockMvc.perform(MockMvcRequestBuilders.multipart("/members/{memberId}/documents", MEMBER_ID)
        .file(mockImageFile)
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNoContent());

    HouseMember member = houseMemberRepository.findByMemberId(MEMBER_ID).get();
    assertEquals(member.getHouseMemberDocument().getDocumentFilename(), String.format("member_%s_document.jpg", MEMBER_ID));
  }

  @Test
  void addHouseMemberDocumentMemberNotExists() throws Exception {
    byte[] imageBytes = getImageAsByteArray(1000, 1000);
    MockMultipartFile mockImageFile = new MockMultipartFile("memberDocument", imageBytes);

    mockMvc.perform(MockMvcRequestBuilders.multipart("/members/{memberId}/documents", "non-exist-member-id")
        .file(mockImageFile)
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNotFound());

    HouseMember member = houseMemberRepository.findByMemberId(MEMBER_ID).get();
    assertNull(member.getHouseMemberDocument());
  }

  @Test
  void addHouseMemberDocumentTooLargeFile() throws Exception {
    byte[] imageBytes = getImageAsByteArray(1000, 1000);
    MockMultipartFile mockImageFile = new MockMultipartFile("memberDocument", imageBytes);

    mockMvc.perform(MockMvcRequestBuilders.multipart("/members/{memberId}/documents", MEMBER_ID)
        .file(mockImageFile)
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNotFound());

    HouseMember member = houseMemberRepository.findByMemberId(MEMBER_ID).get();
    assertNull(member.getHouseMemberDocument());
  }

  @Test
  void putHouseMemberDocumentSuccess() throws Exception {
    byte[] imageBytes = getImageAsByteArray(10, 10);
    MockMultipartFile mockImageFile = new MockMultipartFile("memberDocument", imageBytes);

    addDefaultHouseMemberDocument();

    mockMvc.perform(MockMvcRequestBuilders.multipart("/members/{memberId}/documents", MEMBER_ID)
        .file(mockImageFile)
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNoContent());

    HouseMember member = houseMemberRepository.findByMemberId(MEMBER_ID).get();
    assertEquals(member.getHouseMemberDocument().getDocumentFilename(), String.format("member_%s_document.jpg", member.getMemberId()));
  }

  @Test
  void putHouseMemberDocumentMemberNotExists() throws Exception {
    byte[] imageBytes = getImageAsByteArray(10, 10);
    MockMultipartFile mockImageFile = new MockMultipartFile("memberDocument", imageBytes);

    addDefaultHouseMemberDocument();

    mockMvc.perform(MockMvcRequestBuilders.multipart("/members/{memberId}/documents", "non-exist-member-id")
        .file(mockImageFile)
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  void putHouseMemberDocumentTooLargeFile() throws Exception {
    byte[] imageBytes = getImageAsByteArray(1000, 1000);
    MockMultipartFile mockImageFile = new MockMultipartFile("memberDocument", imageBytes);

    addDefaultHouseMemberDocument();

    mockMvc.perform(MockMvcRequestBuilders.multipart("/members/{memberId}/documents", MEMBER_ID)
        .file(mockImageFile)
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNotFound());

    HouseMember member = houseMemberRepository.findByMemberId(MEMBER_ID).get();
    assertNotEquals(member.getHouseMemberDocument().getDocumentFilename(), String.format("member_%s_document.jpg", member.getMemberId()));
  }

  @Test
  void deleteHouseMemberDocumentSuccess() throws Exception {
    addDefaultHouseMemberDocument();

    mockMvc.perform(delete("/members/{memberId}/documents", MEMBER_ID)
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNoContent());

    HouseMember member = houseMemberRepository.findByMemberId(MEMBER_ID).get();
    assertNull(member.getHouseMemberDocument());
  }

  @Test
  void deleteHouseMemberDocumentMemberNotExists() throws Exception {
    addDefaultHouseMemberDocument();

    mockMvc.perform(delete("/members/{memberId}/documents", "non-existing-member-id")
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteHouseMemberDocumentNoDocumentPresent() throws Exception {
    mockMvc.perform(delete("/members/{memberId}/documents", MEMBER_ID)
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNotFound());
  }


  @Test
  void getHouseMemberDocumentNoDocumentPresent() throws Exception {
    mockMvc.perform(get("/members/{memberId}/documents", MEMBER_ID)
        .headers(getHttpEntityHeaders()))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  private void addDefaultHouseMemberDocument() throws IOException {
    byte[] imageBytes = getImageAsByteArray(10, 10);
    HouseMemberDocument houseMemberDocument = houseMemberDocumentRepository.save(new HouseMemberDocument(TEST_DOCUMENT_NAME, imageBytes));
    HouseMember member = houseMemberRepository.findByMemberId(MEMBER_ID).get();
    member.setHouseMemberDocument(houseMemberDocument);
    houseMemberRepository.save(member);
  }

  private void authDefaultUser() {
    if (userRepository.findByEmail(testUserEmail) == null) {
      CreateUserRequest createUserRequest = new CreateUserRequest(testUserName, testUserEmail, testUserPassword);
      sendRequest(HttpMethod.POST, "users", createUserRequest);
    }
    updateJwtToken(new LoginUserRequest(testUserEmail, testUserPassword));
  }

  private byte[] getImageAsByteArray(int height, int width) throws IOException {
    BufferedImage documentImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    try (ByteArrayOutputStream imageBytesStream = new ByteArrayOutputStream()) {
      ImageIO.write(documentImage, "jpg", imageBytesStream);
      return imageBytesStream.toByteArray();
    }
  }

}

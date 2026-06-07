package com.example.kproject.controller;

import com.example.kproject.security.AuthMemberResolver;
import com.example.kproject.dto.upload.ChatUploadResponse;
import com.example.kproject.service.upload.ChatUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Tag(
        name = "업로드",
        description = "txt, 이미지 등 비정형 대화 파일을 OpenAI로 정형화하고 reportId를 생성하는 API"
)
@RestController
@RequestMapping("/api/uploads")
public class ChatUploadPlatformController {

    private final ChatUploadService chatUploadService;
    private final AuthMemberResolver authMemberResolver;

    public ChatUploadPlatformController(
            ChatUploadService chatUploadService,
            AuthMemberResolver authMemberResolver
    ) {
        this.chatUploadService = chatUploadService;
        this.authMemberResolver = authMemberResolver;
    }

    @Operation(
            summary = "비정형 대화 파일 업로드 및 정형화",
            description = """
                    카카오톡 txt, 복사된 대화 txt, 채팅 캡처 이미지 등 비정형 대화 파일을 업로드합니다.
                    서버는 OpenAI를 우선 사용해 원본을 participants/messages/keywords/rawText 구조의 normalized conversation으로 변환합니다.
                    OpenAI API 키가 없거나 정형화에 실패하면 로컬 파서 기반 fallback으로 가능한 범위의 대화 데이터를 생성합니다.
                    정형화된 결과는 reports 테이블에 저장되고, 응답으로 반환된 reportId를 사용해 요약/관계/성격/선호/인사이트 분석 API를 조회합니다.
                    """
    )
    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatUploadResponse uploadChat(
            @Parameter(
                    description = "업로드할 원본 파일. txt, png, jpg, jpeg, webp 형식을 지원하며 txt는 전체 텍스트, 이미지는 OpenAI vision 입력으로 정형화합니다.",
                    required = true
            )
            @RequestParam("file") MultipartFile file,
            @Parameter(
                    description = "분석 카테고리. 예: 썸, 연애, 친구, 직장, 가족, 일반 분석. 비어 있으면 일반 분석으로 저장합니다.",
                    example = "썸"
            )
            @RequestParam(value = "category", required = false) String category,
            @Parameter(
                    description = "상대방 또는 분석 대상 이름 힌트. OpenAI 정형화와 발신자 추정에 사용합니다.",
                    example = "노지섭"
            )
            @RequestParam(value = "targetName", required = false) String targetName,
            @RequestParam(value = "myName", required = false) String myName,
            @Parameter(
                    description = "사용자가 특히 궁금해하는 분석 요청. 예: 이 사람 진심이 뭘까요?",
                    example = "상대방이 나에게 호감이 있는지 알고 싶어요."
            )
            @RequestParam(value = "description", required = false) String description,
            @Parameter(description = "로그인 사용자의 memberId. 없으면 게스트 리포트로 저장합니다.", example = "1")
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        return chatUploadService.upload(file, category, targetName, myName, description, resolveMemberId(memberId));
    }

    @Operation(
            summary = "여러 대화 파일/캡처 업로드 및 정형화",
            description = """
                    카카오톡 txt 파일 1개, 여러 장의 채팅 캡처 이미지, 또는 txt와 이미지를 함께 업로드합니다.
                    files 배열 순서가 캡처 읽기 순서로 사용되므로 프론트는 사용자가 고른 순서 그대로 전송해야 합니다.
                    OpenAI vision/text 정형화를 우선 사용하며, 사용할 수 없으면 텍스트 기반 fallback으로 리포트를 생성합니다.
                    """
    )
    @PostMapping(value = "/chat/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatUploadResponse uploadChatBatch(
            @Parameter(description = "업로드할 파일 목록. txt, png, jpg, jpeg, webp 형식을 지원합니다.", required = true)
            @RequestParam("files") List<MultipartFile> files,
            @Parameter(description = "분석 카테고리. 예: 썸, 재회, 환승, 소개팅", example = "썸")
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "상대방 또는 분석 대상 이름 힌트", example = "노지섭")
            @RequestParam(value = "targetName", required = false) String targetName,
            @RequestParam(value = "myName", required = false) String myName,
            @Parameter(description = "사용자가 특히 궁금해하는 분석 요청", example = "상대방이 나에게 호감이 있는지 알고 싶어요.")
            @RequestParam(value = "description", required = false) String description,
            @Parameter(description = "로그인 사용자의 memberId. 없으면 게스트 리포트로 저장합니다.", example = "1")
            @RequestParam(value = "memberId", required = false) Long memberId
    ) {
        return chatUploadService.uploadBatch(files, category, targetName, myName, description, resolveMemberId(memberId));
    }

    private Long resolveMemberId(Long fallbackMemberId) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String authorizationHeader = attributes == null
                ? null
                : attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        return authMemberResolver.resolveMemberId(authorizationHeader, fallbackMemberId);
    }
}

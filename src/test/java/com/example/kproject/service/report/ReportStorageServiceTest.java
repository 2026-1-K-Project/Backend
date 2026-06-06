package com.example.kproject.service.report;

import com.example.kproject.domain.ChatSourceType;
import com.example.kproject.domain.ConversationReport;
import com.example.kproject.domain.NormalizedConversationResult;
import com.example.kproject.dto.normalize.NormalizedConversationDto;
import com.example.kproject.dto.report.ReportAnalysisMode;
import com.example.kproject.repository.ConversationReportRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class ReportStorageServiceTest {

    @Test
    void storeReportWithNormalizedConversationJson() throws Exception {
        ConversationReportRepository repository = Mockito.mock(ConversationReportRepository.class);
        Mockito.when(repository.save(any(ConversationReport.class)))
                .thenAnswer(invocation -> {
                    ConversationReport report = invocation.getArgument(0);
                    Field field = ConversationReport.class.getDeclaredField("id");
                    field.setAccessible(true);
                    field.set(report, 10L);
                    return report;
                });

        ReportStorageService service = new ReportStorageService(repository, new ObjectMapper());
        NormalizedConversationDto conversation = new NormalizedConversationDto(
                List.of("사용자", "상대방"),
                List.of(new NormalizedConversationDto.MessageDto(
                        "사용자",
                        "2026-04-29T10:48:00",
                        "오늘 뭐해?",
                        "TEXT"
                )),
                List.of("오늘"),
                "raw text"
        );

        ConversationReport saved = service.createReport(
                "썸/연애",
                ChatSourceType.TXT,
                new NormalizedConversationResult(conversation, ReportAnalysisMode.STRUCTURED, true, null)
        );

        ArgumentCaptor<ConversationReport> captor = ArgumentCaptor.forClass(ConversationReport.class);
        Mockito.verify(repository).save(captor.capture());

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(captor.getValue().getCategory()).isEqualTo("썸/연애");
        assertThat(captor.getValue().getSourceType()).isEqualTo("TXT");
        assertThat(captor.getValue().getNormalizedJson()).contains("오늘 뭐해?");
        assertThat(captor.getValue().getParticipantsJson()).contains("상대방");
        assertThat(captor.getValue().getMessageCount()).isEqualTo(1);
    }

    @Test
    void listAndTrashReportsByMember() throws Exception {
        ConversationReportRepository repository = Mockito.mock(ConversationReportRepository.class);
        ReportStorageService service = new ReportStorageService(repository, new ObjectMapper());
        ConversationReport report = report("썸", 7L);
        setId(report, 20L);

        Mockito.when(repository.findByMemberIdAndTrashedFalseOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(report));
        Mockito.when(repository.findById(20L)).thenReturn(Optional.of(report));

        assertThat(service.listReports(7L, false)).hasSize(1);
        assertThat(service.listReports(7L, false).get(0).memberId()).isEqualTo(7L);

        service.moveToTrash(20L);

        assertThat(report.isTrashed()).isTrue();
        assertThat(report.getTrashedAt()).isNotNull();

        service.restore(20L);

        assertThat(report.isTrashed()).isFalse();
        assertThat(report.getTrashedAt()).isNull();
    }

    private ConversationReport report(String category, Long memberId) {
        NormalizedConversationDto conversation = new NormalizedConversationDto(
                List.of("사용자", "상대방"),
                List.of(new NormalizedConversationDto.MessageDto(
                        "사용자",
                        "2026-04-29T10:48:00",
                        "오늘 뭐해?",
                        "TEXT"
                )),
                List.of("오늘"),
                "raw text"
        );
        return new ConversationReport(
                category,
                ChatSourceType.TXT,
                conversation.rawText(),
                "{}",
                "[\"사용자\",\"상대방\"]",
                conversation.messages().size(),
                com.example.kproject.domain.ReportStatus.COMPLETED,
                ReportAnalysisMode.STRUCTURED.name(),
                null,
                "상대방 호감 분석",
                1,
                memberId
        );
    }

    private void setId(ConversationReport report, Long id) throws Exception {
        Field field = ConversationReport.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(report, id);
    }
}

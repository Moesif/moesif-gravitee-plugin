package io.gravitee.reporter.file.vertx;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.common.MetricsType;
import io.gravitee.reporter.common.formatter.Formatter;
import io.gravitee.reporter.file.config.FileReporterConfiguration;
import io.vertx.core.Vertx;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VertxFileWriterTest {

    private static final ZoneOffset ZONE = ZoneOffset.UTC;
    private static final LocalDateTime NOW = LocalDateTime.now(ZONE);

    private VertxFileWriter<Reportable> vertxFileWriter;

    @Mock
    private FileReporterConfiguration configuration;

    @Mock
    private File file;

    @Before
    public void setup() {
        when(configuration.getFilename()).thenReturn("/metrics/%s-yyyy_mm_dd");
        vertxFileWriter = new VertxFileWriter<>(mock(Vertx.class), MetricsType.REQUEST, mock(Formatter.class), "filename", configuration);
    }

    @Test
    public void shouldDeleteFile_should_return_true_if_retainDays_configuration_exceeds_file_lastModified_time() {
        when(configuration.getRetainDays()).thenReturn(10L);

        long currentTimeMs = NOW.toInstant(ZONE).toEpochMilli();
        when(file.lastModified()).thenReturn(NOW.toInstant(ZONE).minus(10, DAYS).minus(1, SECONDS).toEpochMilli());

        assertTrue(vertxFileWriter.shouldDeleteFile(file, currentTimeMs));
    }

    @Test
    public void shouldDeleteFile_should_return_false_if_retainDays_configuration_doesnt_exceed_file_lastModified_time() {
        when(configuration.getRetainDays()).thenReturn(10L);

        long currentTimeMs = NOW.toInstant(ZONE).toEpochMilli();
        when(file.lastModified()).thenReturn(NOW.toInstant(ZONE).minus(10, DAYS).plus(1, SECONDS).toEpochMilli());

        assertFalse(vertxFileWriter.shouldDeleteFile(file, currentTimeMs));
    }

    @Test
    public void shouldDeleteFile_should_return_false_if_retainDays_configuration_is_0() {
        when(configuration.getRetainDays()).thenReturn(0L);

        long currentTimeMs = NOW.toInstant(ZONE).toEpochMilli();

        assertFalse(vertxFileWriter.shouldDeleteFile(file, currentTimeMs));
        verify(file, never()).lastModified();
    }
}

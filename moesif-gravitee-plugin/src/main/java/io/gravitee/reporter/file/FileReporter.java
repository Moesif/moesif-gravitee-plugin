package io.gravitee.reporter.file;

import io.gravitee.common.service.AbstractService;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.Reporter;
import io.gravitee.reporter.common.MetricsType;
import io.gravitee.reporter.common.formatter.Formatter;
import io.gravitee.reporter.common.formatter.FormatterFactory;
import io.gravitee.reporter.file.config.FileReporterConfiguration;
import io.gravitee.reporter.file.vertx.VertxFileWriter;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileReporter extends AbstractService<Reporter> implements Reporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReporter.class);

    private final FileReporterConfiguration configuration;

    private final FormatterFactory formatterFactory;

    private final Vertx vertx;

    private final Map<Class<? extends Reportable>, VertxFileWriter<Reportable>> writers = new HashMap<>(4);

    public FileReporter(FileReporterConfiguration configuration, Vertx vertx, FormatterFactory formatterFactory) {
        this.formatterFactory = formatterFactory;
        this.configuration = configuration;
        this.vertx = vertx;
    }

    @Override
    public void report(Reportable reportable) {
        writers.get(reportable.getClass()).write(reportable);
    }

    @Override
    public boolean canHandle(Reportable reportable) {
        return configuration.isEnabled() && writers.containsKey(reportable.getClass());
    }

    @Override
    protected void doStart() {
        if (configuration.isEnabled()) {
            // Initialize reporters
            for (MetricsType type : MetricsType.values()) {
                Formatter<Reportable> formatter = formatterFactory.getFormatter(configuration.getOutputType(), type);
                applicationContext.getAutowireCapableBeanFactory().autowireBean(formatter);
                writers.put(
                    type.getClazz(),
                    new VertxFileWriter<>(
                        vertx,
                        type,
                        formatter,
                        configuration.getFilename() + '.' + configuration.getOutputType().getExtension(),
                        configuration
                    )
                );
            }

            Future
                .join(writers.values().stream().map(VertxFileWriter::initialize).toList())
                .onComplete(event -> {
                    if (event.succeeded()) {
                        LOGGER.info("File reporter successfully started");
                    } else {
                        LOGGER.info("An error occurs while starting file reporter", event.cause());
                    }
                });
        }
    }

    @Override
    protected void doStop() {
        if (configuration.isEnabled()) {
            Future
                .join(writers.values().stream().map(VertxFileWriter::stop).toList())
                .onComplete(event -> {
                    if (event.succeeded()) {
                        LOGGER.info("File reporter successfully stopped");
                    } else {
                        LOGGER.info("An error occurs while stopping file reporter", event.cause());
                    }
                });
        }
    }
}

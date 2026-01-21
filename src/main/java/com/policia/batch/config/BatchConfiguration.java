package com.policia.batch.config;

import com.policia.batch.listener.BatchErrorListener;
import com.policia.batch.model.PoliciaData;
import com.policia.batch.processor.PoliciaDataProcessor;
import com.policia.batch.reader.IBMMQItemReader;
import com.policia.batch.writer.PoliciaDataWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final IBMMQItemReader itemReader;
    private final PoliciaDataProcessor itemProcessor;
    private final PoliciaDataWriter itemWriter;
    private final BatchErrorListener batchErrorListener;

    @Autowired
    public BatchConfiguration(JobBuilderFactory jobBuilderFactory,
                            StepBuilderFactory stepBuilderFactory,
                            IBMMQItemReader itemReader,
                            PoliciaDataProcessor itemProcessor,
                            PoliciaDataWriter itemWriter,
                            BatchErrorListener batchErrorListener) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.itemReader = itemReader;
        this.itemProcessor = itemProcessor;
        this.itemWriter = itemWriter;
        this.batchErrorListener = batchErrorListener;
    }

    @Bean
    public Job policiaXmlProcessingJob() {
        return jobBuilderFactory.get("policiaXmlProcessingJob")
                .start(processXmlStep())
                .listener(batchErrorListener)
                .build();
    }

    @Bean
    public Step processXmlStep() {
        return stepBuilderFactory.get("processXmlStep")
                .<String, PoliciaData>chunk(1) // Chunk size de 1 para control estricto
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .faultTolerant()
                .skipPolicy(new CustomSkipPolicy()) // Política personalizada que NO salta errores críticos
                .listener(batchErrorListener)
                .build();
    }
}

package com.sstec.cdk.cicd;

import com.sstec.cdk.cicd.stacks.*;
import software.amazon.awscdk.core.App;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CICDTest {
    private final static ObjectMapper JSON =
        new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

    @Test
    public void testStack() throws Exception {
        App app = new App();
        MultiStageStackProps devStackProps = new MultiStageStackProps();
        FargateStack f = new FargateStack(app, "fargate", devStackProps);
//        S3Stack stack = new S3Stack(app, "test-dev-stack", devStackProps);

//        JsonNode actual = JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());
//        assertThat(new ObjectMapper().createObjectNode()).isEqualTo(actual);
    }
}

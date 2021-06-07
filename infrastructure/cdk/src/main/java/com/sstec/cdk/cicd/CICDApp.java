package com.sstec.cdk.cicd;

import com.sstec.cdk.cicd.stacks.*;
import software.amazon.awscdk.core.App;

/**
 * Sample application for testing CI and CD
 */
public class CICDApp {

    public static void main(final String[] args) throws Exception{
        App app = new App();
        MultiStageStackProps stackProps = new MultiStageStackProps();

        if (stackProps.isInfrastructure) {
            new ECRStack(app, "ECR", stackProps);
            new CodeCommitStack(app, "CodeCommit", stackProps);
        } else {
            new S3Stack(app, "S3", stackProps);
            new FargateStack(app, "Fargate", stackProps);
            new CodePipelineStack(app, "CodePipeline", stackProps);
        }


        app.synth();
    }
}

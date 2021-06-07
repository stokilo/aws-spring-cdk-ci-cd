package com.sstec.cdk.cicd.stacks;

import com.sstec.cdk.cicd.MultiStageStackProps;
import com.sstec.cdk.cicd.Tagging;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.s3.*;

import java.util.Collections;


public class S3Stack extends Stack {

    public S3Stack(final Construct scope, final String id, final MultiStageStackProps props) {
        super(scope, id, props.props);

        props.ecrBucket = new Bucket(this, "aws-ci-cd-ecr-bucket", new BucketProps.Builder()
                .versioned(true)
                .publicReadAccess(false)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .encryption(BucketEncryption.KMS_MANAGED)
                .lifecycleRules(Collections.singletonList(
                        LifecycleRule.builder().expiration(Duration.days(3)).build()
                ))
                .build());
        Tagging.addEnvironmentTag(props.ecrBucket, props);

    }
}

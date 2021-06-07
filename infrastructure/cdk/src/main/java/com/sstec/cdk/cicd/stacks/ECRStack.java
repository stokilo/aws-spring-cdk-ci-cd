package com.sstec.cdk.cicd.stacks;

import com.sstec.cdk.cicd.MultiStageStackProps;
import com.sstec.cdk.cicd.Tagging;
import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.iam.AccountPrincipal;
import software.amazon.awscdk.services.iam.AnyPrincipal;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;

import java.util.Collections;


public class ECRStack extends Stack {

    public static final String REPOSITORY_NAME = "ecr-aws-spring-cdk-ci-cd";

    public ECRStack(final Construct scope, final String id, final MultiStageStackProps props) {
        super(scope, id, props.props);

        Repository ecrRepository = Repository.Builder.create(this, ECRStack.REPOSITORY_NAME)
                .repositoryName(ECRStack.REPOSITORY_NAME)
                .removalPolicy(RemovalPolicy.DESTROY)
                .lifecycleRules(Collections.singletonList(LifecycleRule.builder().maxImageAge(Duration.days(3)).build()))
                .build();

        ecrRepository.addToResourcePolicy(
                PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .principals(Collections.singletonList(new AccountPrincipal(props.ciCdAccountId)))
                        .actions(Collections.singletonList("ecr:*"))
                        .build()
        );


        Tagging.addEnvironmentTag(ecrRepository, props);

    }
}

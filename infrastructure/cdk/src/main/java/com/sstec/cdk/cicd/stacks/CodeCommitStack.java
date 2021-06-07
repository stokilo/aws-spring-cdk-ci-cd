package com.sstec.cdk.cicd.stacks;

import com.sstec.cdk.cicd.MultiStageStackProps;
import com.sstec.cdk.cicd.Tagging;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.codecommit.OnCommitOptions;
import software.amazon.awscdk.services.codecommit.Repository;
import software.amazon.awscdk.services.events.targets.EventBus;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.ssm.ParameterType;
import software.amazon.awscdk.services.ssm.StringParameter;

import java.util.Arrays;
import java.util.Collections;


public class CodeCommitStack extends Stack {

    public static final String REPOSITORY_NAME = "cc-aws-spring-cdk-ci-cd";
    public static final String CODE_COMMIT_ACCESS_ROLE = "CodeCommitAccessRole";

    public CodeCommitStack(final Construct scope, final String id, final MultiStageStackProps props) {
        super(scope, id, props.props);

        Repository codeCommitRepository = Repository.Builder
                .create(this, CodeCommitStack.REPOSITORY_NAME)
                .repositoryName(CodeCommitStack.REPOSITORY_NAME)
                .description("AWS Spring CI/CD project")
                .build();

        Role codeCommitAccessRole = new Role(this, CODE_COMMIT_ACCESS_ROLE, RoleProps.builder()
                .description("Role for accessing CodeCommit repository")
                .assumedBy(new AccountPrincipal(props.ciCdAccountId))
                .roleName(CODE_COMMIT_ACCESS_ROLE)
                .build());

        codeCommitAccessRole.addToPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .resources(Collections.singletonList(codeCommitRepository.getRepositoryArn()))
                .actions(Arrays.asList(
                        "codecommit:GitPull",
                        "codecommit:GetBranch",
                        "codecommit:GetCommit",
                        "codecommit:ListBranches",
                        "codecommit:GetUploadArchiveStatus",
                        "codecommit:ListRepositories",
                        "codecommit:UploadArchive",
                        "codecommit:GetCommitHistory"
                        ))
                .build());

                codeCommitAccessRole.addToPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .resources(Arrays.asList(
                        "arn:aws:s3:::codepipeline-*",
                        "arn:aws:s3:::codepipeline-*/*",
                        String.format("arn:aws:kms:%s:%s:key/*", props.region, props.ciCdAccountId)
                ))
                .actions(Arrays.asList(
                        "s3:GetObject*",
                        "s3:GetBucket*",
                        "s3:List*",
                        "s3:DeleteObject*",
                        "s3:PutObject*",
                        "s3:Abort*",
                        "kms:Decrypt",
                        "kms:DescribeKey",
                        "kms:Encrypt",
                        "kms:ReEncrypt*",
                        "kms:GenerateDataKey*"
                ))
                .build());


        Tagging.addEnvironmentTag(codeCommitRepository, props);

    }
}

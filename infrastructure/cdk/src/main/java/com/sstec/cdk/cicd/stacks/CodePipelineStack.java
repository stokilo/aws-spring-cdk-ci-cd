package com.sstec.cdk.cicd.stacks;

import com.sstec.cdk.cicd.MultiStageStackProps;
import com.sstec.cdk.cicd.Tagging;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.codebuild.*;
import software.amazon.awscdk.services.codepipeline.Artifact;
import software.amazon.awscdk.services.codepipeline.Pipeline;
import software.amazon.awscdk.services.codepipeline.StageProps;
import software.amazon.awscdk.services.codepipeline.actions.*;
import software.amazon.awscdk.services.iam.*;

import java.util.*;


public class CodePipelineStack extends Stack {

    public CodePipelineStack(final Construct scope, final String id, final MultiStageStackProps props) {
        super(scope, id, props.props);

        software.amazon.awscdk.services.codecommit.IRepository codeCommitRepo =
                software.amazon.awscdk.services.codecommit.Repository.fromRepositoryArn(
                        this, CodeCommitStack.REPOSITORY_NAME,
                        String.format("arn:aws:codecommit:%s:%s:%s",
                                props.region, props.infrastructureAccountId, CodeCommitStack.REPOSITORY_NAME));

        software.amazon.awscdk.services.ecr.IRepository ecrRepo =
                software.amazon.awscdk.services.ecr.Repository.fromRepositoryArn(
                        this, ECRStack.REPOSITORY_NAME,
                        String.format("arn:aws:ecr:%s:%s:%s",
                                props.region, props.infrastructureAccountId, ECRStack.REPOSITORY_NAME));

        IRole infrastructureAccessRole = Role.fromRoleArn(this, "infrastructureAccessRole",
                String.format("arn:aws:iam::%s:role/%s",
                        props.infrastructureAccountId, CodeCommitStack.CODE_COMMIT_ACCESS_ROLE));


        Role rolePipelineProjectBuild = new Role(this, "RolePipelineProjectBuild", RoleProps.builder()
                .description("Role for code build project")
                .assumedBy(new ServicePrincipal("codebuild.amazonaws.com"))
                .roleName("RolePipelineProjectBuild")
                .build());

        // by referencing ECR image in the Dockerfile.FROM, it was required to add policy for /repository resource path
        rolePipelineProjectBuild.addToPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .resources(Arrays.asList(
                              ecrRepo.getRepositoryArn(),
                              String.format("arn:aws:ecr:%s:%s:repository/%s",
                                 props.region, props.infrastructureAccountId, ECRStack.REPOSITORY_NAME))
                )
                .actions(Arrays.asList(
                        "ecr:BatchCheckLayerAvailability",
                        "ecr:GetDownloadUrlForLayer",
                        "ecr:GetRepositoryPolicy",
                        "ecr:DescribeRepositories",
                        "ecr:ListImages",
                        "ecr:DescribeImages",
                        "ecr:BatchGetImage",
                        "ecr:ListTagsForResource",
                        "ecr:DescribeImageScanFindings",
                        "ecr:InitiateLayerUpload",
                        "ecr:UploadLayerPart",
                        "ecr:CompleteLayerUpload",
                        "ecr:PutImage",
                        "ecr:GetAuthorizationToken"
                ))
                .conditions(new HashMap<String, Object>() {{
                    put("StringEquals", new HashMap<String, String>(){{
                        put(String.format("ecr:ResourceTag/app:%s:env", props.appName), MultiStageStackProps.ENV_INFRASTRUCTURE);
                    }});
                }})
                .build());

        rolePipelineProjectBuild.addToPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .resources(Collections.singletonList("*"))
                .actions(Collections.singletonList(
                        "ecr:GetAuthorizationToken"
                ))
                .build());

        Artifact sourceOutput = new Artifact("CdkSourceOutput");
        Artifact cdkBuildOutput = new Artifact("CdkBuildOutput");

        PipelineProject cdkBuildProject = PipelineProject.Builder.create(this, "codeBuildProject")
                .projectName("aws-spring-cdk-ci-cd")
                .environment(BuildEnvironment.builder().privileged(true).buildImage(LinuxBuildImage.AMAZON_LINUX_2_3).build())
                .environmentVariables(new HashMap<String, BuildEnvironmentVariable>() {{
                    put("AWS_DEFAULT_REGION", BuildEnvironmentVariable.builder()
                            .type(BuildEnvironmentVariableType.PLAINTEXT)
                            .value(props.region)
                            .build());
                    put("AWS_ACCOUNT_ID", BuildEnvironmentVariable.builder()
                            .type(BuildEnvironmentVariableType.PLAINTEXT)
                            .value(props.infrastructureAccountId)
                            .build());
                    put("IMAGE_REPO_NAME", BuildEnvironmentVariable.builder()
                            .type(BuildEnvironmentVariableType.PLAINTEXT)
                            .value(ECRStack.REPOSITORY_NAME)
                            .build());
                    put("IMAGE_TAG", BuildEnvironmentVariable.builder()
                            .type(BuildEnvironmentVariableType.PLAINTEXT)
                            .value("latest")
                            .build());

                }})
                .buildSpec(BuildSpec.fromSourceFilename("buildspec-docker.yml"))
                .role(rolePipelineProjectBuild)
                .build();


        Pipeline ciCdPipeline = Pipeline.Builder.create(this, "code-pipeline").stages(
                Arrays.asList(
                        StageProps.builder()
                                .stageName("CodeCommitStage")
                                .actions(Collections.singletonList(
                                        CodeCommitSourceAction.Builder.create()
                                                .actionName("CodeCommitSource")
                                                .repository(codeCommitRepo)
                                                .output(sourceOutput)
                                                .branch("master")
                                                .role(infrastructureAccessRole)
                                                .trigger(CodeCommitTrigger.POLL)
                                                .build()
                                ))
                                .build(),
                        StageProps.builder()
                                .stageName("CodeBuildStage")
                                .actions(Collections.singletonList(
                                        CodeBuildAction.Builder.create()
                                                .actionName("CodeBuildBuild")
                                                .project(cdkBuildProject)
                                                .input(sourceOutput)
                                                .outputs(Collections.singletonList(cdkBuildOutput))
                                                .build()
                                ))
                                .build()

                          , StageProps.builder().stageName("CodeDeployStage")
                                .actions(Collections.singletonList(
                                        EcsDeployAction.Builder.create()
                                                .actionName("CodeDeployStage")
                                                .input(cdkBuildOutput)
                                                .service(props.applicationLoadBalancedFargateService.getService())
                                                .build()
                                ))
                                .build()
                )
        )
        .crossAccountKeys(true)
        .pipelineName("ci-cd-pipeline")
        .build();

        Tagging.addEnvironmentTag(cdkBuildProject, props);
        Tagging.addEnvironmentTag(ciCdPipeline, props);
        Tagging.addEnvironmentTag(rolePipelineProjectBuild, props);

    }
}

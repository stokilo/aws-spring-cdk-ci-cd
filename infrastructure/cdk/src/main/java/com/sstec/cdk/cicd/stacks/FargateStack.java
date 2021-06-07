package com.sstec.cdk.cicd.stacks;

import com.sstec.cdk.cicd.MultiStageStackProps;
import com.sstec.cdk.cicd.Tagging;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.logs.LogGroup;

import java.util.Arrays;
import java.util.HashMap;

public class FargateStack extends Stack {

    public FargateStack(final Construct scope, final String id, final MultiStageStackProps props) {
        super(scope, id, props.props);

        software.amazon.awscdk.services.ecr.IRepository ecrRepo =
                software.amazon.awscdk.services.ecr.Repository.fromRepositoryArn(
                        this, ECRStack.REPOSITORY_NAME,
                        String.format("arn:aws:ecr:%s:%s:%s",
                                props.region, props.infrastructureAccountId, ECRStack.REPOSITORY_NAME));

        Cluster cluster = Cluster.Builder.create(this, "ecs-cluster")
                .clusterName("ecs-cluster")
                .build();

        ExplicitEcrImage explicitEcrImage = new ExplicitEcrImage(ecrRepo, "latest", props.infrastructureAccountId,
                props.region, ECRStack.REPOSITORY_NAME);

        props.applicationLoadBalancedFargateService = ApplicationLoadBalancedFargateService
                .Builder.create(this, "ecs-fargate")
                .cluster(cluster)
                .cpu(1024)
                .memoryLimitMiB(2048)
                .desiredCount(1)
                .publicLoadBalancer(true)
                .assignPublicIp(true)
                .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                        .image(explicitEcrImage)
                        .enableLogging(true)
                        .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder().logGroup(
                                LogGroup.Builder
                                        .create(this, "ecs-log-group")
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .logGroupName("ecs-log-group")
                                        .build())
                                .streamPrefix("-fargate").build()))
                        .containerPort(8080)
                        .build())
                .build();


        props.applicationLoadBalancedFargateService.getTargetGroup().configureHealthCheck(HealthCheck.builder()
                .healthyHttpCodes("200")
                .path("/")
                .port("8080")
                .build());

        ScalableTaskCount scalableTaskCount = props.applicationLoadBalancedFargateService.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(1)
                .maxCapacity(1)
                .build());

        scalableTaskCount.scaleOnCpuUtilization("scale-cpu",
                CpuUtilizationScalingProps.builder()
                        .targetUtilizationPercent(30)
                        .build());

        props.applicationLoadBalancedFargateService.getTaskDefinition().getExecutionRole().addToPrincipalPolicy(PolicyStatement.Builder.create()
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

        Tagging.addEnvironmentTag(cluster, props);
        Tagging.addEnvironmentTag(props.applicationLoadBalancedFargateService, props);
        Tagging.addEnvironmentTag(scalableTaskCount, props);
    }
}

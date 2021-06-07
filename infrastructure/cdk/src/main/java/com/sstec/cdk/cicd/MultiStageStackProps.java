package com.sstec.cdk.cicd;

import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.s3.Bucket;

public class MultiStageStackProps {

    public static final String ENV_CICD = "cicd";

    public static final String ENV_INFRASTRUCTURE = "infrastructure";

    public String appName = "aws-spring-cdk-ci-cd";

    public String env = "";

    public Boolean isCiCd = false;

    public Boolean isInfrastructure = false;

    public String infrastructureAccountId = "";

    public String ciCdAccountId = "";

    public String region = "";

    public StackProps props;

    public Bucket ecrBucket;

    public ApplicationLoadBalancedFargateService applicationLoadBalancedFargateService;

    public MultiStageStackProps() throws Exception{
        this.env = System.getenv("ENV");

        this.props = StackProps.builder().env(makeEnv()).build();

        this.isCiCd = this.env.equalsIgnoreCase(ENV_CICD);
        this.isInfrastructure = this.env.equalsIgnoreCase(ENV_INFRASTRUCTURE);

        this.infrastructureAccountId = System.getenv("CDK_DEPLOY_INFRASTRUCTURE_ACCOUNT_ID");
        this.ciCdAccountId = System.getenv("CDK_DEPLOY_CICD_ACCOUNT_ID");

        this.region = System.getenv("CDK_DEPLOY_REGION");
    }

    private Environment makeEnv() throws Exception {
        return Environment.builder()
                .account(System.getenv("CDK_DEPLOY_ACCOUNT"))
                .region(System.getenv("CDK_DEPLOY_REGION"))
                .build();
    }

}

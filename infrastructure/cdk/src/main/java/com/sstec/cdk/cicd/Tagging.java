package com.sstec.cdk.cicd;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Resource;
import software.amazon.awscdk.core.TagProps;
import software.amazon.awscdk.core.Tags;
import software.amazon.awscdk.services.ec2.ISubnet;
import software.amazon.awscdk.services.ec2.Vpc;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * My custom tag policy to use namespace prefix for resources tags in form app:{appName}:env
 * AWS services reserves aws: namespace, I want to have separate one grouped per application and environment
 */
public class Tagging {

    /**
     * Group custom tags per application under 'app' namespace.
     *
     * @return key for app specific environment tag i.e. app:{appName}:env
     */
    private static String getEnvKey(MultiStageStackProps props) {
        return String.format("%s:%s:env", "app", props.appName);
    }

    public static void addEnvironmentTag(Vpc vpc, MultiStageStackProps props) {
        Tags.of(vpc).add(getEnvKey(props), props.env, TagProps.builder()
                .applyToLaunchedInstances(true)
                .priority(100).build());

        List<ISubnet> allSubnets = Stream.concat(vpc.getPrivateSubnets().stream(), vpc.getPublicSubnets().stream())
                .collect(Collectors.toList());

        for (ISubnet vpcSubnet : allSubnets) {
            Tags.of(vpcSubnet).add(getEnvKey(props), props.env);
        }
    }

    public static void addEnvironmentTag(Construct construct, MultiStageStackProps props) {
        Tags.of(construct).add(getEnvKey(props), props.env);
    }
}

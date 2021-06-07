package com.sstec.cdk.cicd.stacks;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecs.EcrImage;


/**
 * Override image name. Function fromEcrRepository is not resoling image name correctly in cross account scenario.
 * Observed since CDK 1.105
 */
public class ExplicitEcrImage extends EcrImage {

    private final String accountId;
    private final String region;
    private final String repositoryName;

    public ExplicitEcrImage(@NotNull IRepository repository, @NotNull String tagOrDigest, String accountId,
                            String region, String repositoryName) {
        super(repository, tagOrDigest);
        this.accountId = accountId;
        this.region = region;
        this.repositoryName = repositoryName;
    }


    @Override
    public @NotNull String getImageName() {
        return String.format("%s.dkr.ecr.%s.amazonaws.com/%s", accountId, region, repositoryName);
    }
};
[![Stand With Ukraine](https://raw.githubusercontent.com/vshymanskyy/StandWithUkraine/main/banner2-direct.svg)](https://vshymanskyy.github.io/StandWithUkraine)
#### What is it?

AWS Fargate+SpringBoot CI-CD application provisioned with CDK library.

I've provisioned two stacks here. The first one deploys CodeCommit and ECR repository 
under INFRASTRUCTURE account (111111111111). The second stack deploys CodePipeline
and Fargate cluster under CICD account (222222222222).

On each commit to the CodeCommit repository your Fargate cluster will be updated by CodePipeline job.

Bootstrap scripts includes a support for SSO and requires that this feature is enabled for your accounts.

Please note: this stack is for testing purpose only. Many steps are simplified i.e.

1. Tagging images with 'latest' tag (bad practice)
   
2. SSO with admin permission set 
   
3. SCP policies are not part of IaC (not included)

##### Organization 'STC'

We start with a dummy organization with a name 'STC'. Organization structure is presented below.

    Root
      -> STC
        -> INFRASTRUCTURE (account 111111111111)
           ---> CodeCommit 
           ---> ECR 
        -> CICD (account 222222222222)
           ---> CodePipeline
           ---> Fargate cluster
        -> WORKLOADS 
          -> DEV (account 333333333333)
          -> PROD ...
          -> TEST ...

You can provision a similar organization using org-formation CLI.

    https://github.com/org-formation/org-formation-cli

I'm not including my organization here for security reasons. To run samples from this
repository it is enough to have INFRASTRUCTURE and CICD accounts configured. Create sub-accounts using AWS Organizations 
and add OrganizationAccountAccessRole role. This role will be used later in the AWS console for 'Switch Role' feature.

My STC organization includes SCP to disable repository delete actions.
Policy is attached to the STC organization unit. 

Only initial repository deployment is allowed. Repository deletion is forbidden when SCP is activated.
See org-formation CLI for more details how to automate this.

##### User account setup

###### IAM setup

Create IAM admin user (i.e. IAMADMIN) for your STC organization.
Enable MFA for root and IAMADMIN, disable your root user using SCP.

Rest of the configuration perform with the IAMADMIN user.
Use 'Switch Role' functionality on AWS console to configure each account separately.

###### SSO setup

Enable SSO in your region. Examples in this repository are configured in us-east-1 region.
I'm using AWS SSO identity source. Thanks that, my users are administrated from AWS console directly. 

Configure your portal URL i.e.

    https://your-organization.awsapps.com/start

Create new admin user for AWS SSO with AdministratorAccess permission set:

    IAMADMIN-STC
    PermissionSet: AdministratorAccess
    
Assign user to CIDI and INFRASTRUCTURE org units with AdministratorAccess permission set.

Enable MFA for IAMADMIN-STC.

AWS CLI setup is required to perform deployment or to clone git repository.
Run following command once for each account:

    aws configure sso

After this step config file should include at least two new profiles. Each profile is associated with a single account

    cat ~/.aws/config
    ....

    [profile iamadmin-stc-sso-111111111111]
    sso_start_url = https://your-organization.awsapps.com/start
    sso_region = us-east-1
    sso_account_id = 111111111111
    sso_role_name = AdministratorAccess
    region = us-east-1
    output = json

    [profile iamadmin-stc-sso-222222222222]
    sso_start_url = https://your-organization.awsapps.com/start
    sso_region = us-east-1
    sso_account_id = 222222222222
    sso_role_name = AdministratorAccess
    region = us-east-1
    output = json

Use iamadmin-stc-sso-111111111111 to deploy infrastructure and work with CodeCommit git repo.
Use iamadmin-stc-sso-222222222222 for CICD deployment.


After this step, you will have your accounts config ready. You still need session keys.
These will be automatically populated under

    ~/.aws/credentials

while running ./cdk.sh (to be precise, sso.sh, you can run it directly as well)

These shell scripts will open a browser window with portal URL for authentication. Session keys will be stored under

    ~/.aws/sso

and auto-populated in AWS credentials file

    ~/.aws/credentials

That is all. You may perform authentication manually like following:

    ./sso.sh iamadmin-stc-sso-111111111111

#### How to deploy the stack?

Firstly, configure deployment properties file

    cd infrastructure
    cp templates/env.properties env.properties

If you follow SSO setup example values should be like:

    region=us-east-1

    infrastructure.account=111111111111
    infrastructure.sso.profile=iamadmin-stc-sso-111111111111

    cicd.account=222222222222
    cicd.sso.profile=iamadmin-stc-sso-222222222222



I've decided to split setup into two phases, first phase configures CodeCommit and ECR repository. 
Once this is done, you push initial files to git and docker image to ECR respectively.

Second stack deploys CodePipeline and Fargate cluster. Stack is configured to run the pipeline on each commit.

Deploy the infrastructure stack:
 
    ./cdk.sh deploy infrastucture --all

Switch role to INFRASTRUCTURE account on AWS console, select correct region:

    Select  'CodeCommit' -> copy HTTPS {link} 

    git clone {link}  ~/aws-spring-cdk-ci-cd

In case of error or 403 on MacOS:

    # see https://docs.aws.amazon.com/codecommit/latest/userguide/troubleshooting-ch.html
    git config -l --show-origin | grep credential
   
    # i.e.
    sudo vim /Library/Developer/CommandLineTools/usr/share/git-core/gitconfig

    # add credential to the SSO for INFRASTRUCTURE account
    [credential]
    helper = !aws --profile iamadmin-stc-sso-111111111111 codecommit credential-helper $@
    UseHttpPath = true

If your temp session token is expired then run

    ./sso.sh iamadmin-stc-sso-111111111111

try to clone again, add required files, commit and push

    cp pom.xml Dockerfile buildspec-docker.yml src/ ~/aws-spring-cdk-ci-cd
    cd ~/aws-spring-cdk-ci-cd
    git add . && git commit -m "Initial commit" && git push origin master

Push an initial image to the ECR repo. This step ensures that our docker images are fetched from ECR only.
We don't want to fetch images from docker registry using AWS IP. That would require docker authentication to avoid rate
limiting. We keep this example simple.

Run docker daemon on your dev machine. Navigate to the project folder root. Export your SSO username and authenticate
in case session is expired:

    export AWS_PROFILE=iamadmin-stc-sso-111111111111
    # optionally, in case session is expired
    ./infrastructure/sso.sh iamadmin-stc-sso-111111111111

perform docker login, build the image, tag and push

    cd ~/aws-spring-cdk-ci-cd
    aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 111111111111.dkr.ecr.us-east-1.amazonaws.com
    mvn clean install
    docker build -t 111111111111.dkr.ecr.us-east-1.amazonaws.com/ecr-aws-spring-cdk-ci-cd:latest .
    docker push 111111111111.dkr.ecr.us-east-1.amazonaws.com/ecr-aws-spring-cdk-ci-cd:latest

    update Dockerfile content to:

    FROM 111111111111.dkr.ecr.us-east-1.amazonaws.com/ecr-aws-spring-cdk-ci-cd:latest
    USER spring:spring
    ARG JAR_FILE=target/*.jar
    COPY ${JAR_FILE} app.jar
    ENTRYPOINT ["java","-jar","/app.jar"]

    echo "target" > .gitignore
    git add . && git commit -m "Change Dockerfile base image." && git push origin master


Deploy CodePipeline and Fargate cluster

    ./cdk.sh deploy cicd --all

You will find sample SpringBoot application under src/ folder. You can modify code in your working copy

    ~/aws-spring-cdk-ci-cd

Commit your change and push to the master. This is final step. After few minutes CodePipeline will
build a new docker image and deploy to the Fargate cluster.

Switch role to CICD account and navigate to the EC2 Load Balancing -> Load Balancers section. Copy DNS name and open
in the browser i.e.

    curl http://farga-ecsfa-ifjais929292w-AAAAABBBB.us-east-1.elb.amazonaws.com/
    Hello Docker World


#### Lessons learned

1. AWS CDK heavy lifting

AWS CDK is doing a lot for you in the background. This can be misleading in some cases, sometimes following AWS
documentation for resource setup is not required at all. For example, cross-account setup for CDK requires attaching
an imported role from another account. CDK magically attached required policies and resolved CodeCommit repository ARN.
Without that, you would end up with an unusable stack where AWS Console shows you the wrong account number in the CodeCommit
ARN

=> Bookmark AWS CDK Github, join slack, search existing tickets and ask questions

2. AWS CDK pipelines library

AWS CDK list some issues i.e. links on the Console won't work in a cross-account scenario. You should be aware
that it is a limitation, but the stack works perfectly fine.

=> Always check AWS CDK typescript documentation

3. SSO 

Background token refresh on the web AWS Console is not consistently refreshed. You have to reload browser tab manually.
Fortunately, you are not logged out.

Session duration is set to 4 hours, it is ok for regular work use cases.

=> Use SSO whenever possible regardless of small issues and requirement to refresh ~/.aws/credentials with sso.sh 
script

4. Cross account browser workspace

I found the best for my use case to use the same browser with one regular window and one in private mode. 

=> Working with a single browser allows accessing your private bookmarks. Don't trust browser extensions for manage isolated
sessions, this is too risky.

5. ECR repository permission

Docker introduced rate-limiting for unauthorized pull requests. This affects code pipeline workflows, it is easy to
get rate limited on docker pull because requests are executed from AWS network. The suggested workaround is to configure docker 
credentials and use them to log in. This requires managing secrets. I've decided to
push my base images and use them for Dockerfile FROM clause. However, this resulted in a requirement to add to my policy 
ARN with /repository path (see the code). 

=> Keep it simple for testing stacks. 

6. AWS CDK source code

Checkout AWS CDK source code. It is implemented in a typescript language. It is useful for tracing more 
complex issues.

=> Check the source code before you ask the question.

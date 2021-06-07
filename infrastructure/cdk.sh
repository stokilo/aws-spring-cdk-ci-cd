#!/usr/bin/env bash

#set -o xtrace

function prop {
    grep "${1}" env.properties|cut -d'=' -f2
}

ENV=${1:-dev}

if [[ $# -ge 2 ]]; then
    export ENV=$2
    CDK_DEPLOY_INFRASTRUCTURE_ACCOUNT_ID=$(prop "infrastructure.account")
    CDK_DEPLOY_CICD_ACCOUNT_ID=$(prop "cicd.account")
    CDK_DEPLOY_REGION=$(prop "region")

    CDK_DEPLOY_ACCOUNT=$(prop "$ENV.account")
    CDK_PROFILE=$(prop "$ENV.sso.profile")


    export CDK_DEPLOY_ACCOUNT \
           CDK_DEPLOY_REGION \
           CDK_PROFILE \
           CDK_DEPLOY_INFRASTRUCTURE_ACCOUNT_ID \
           CDK_DEPLOY_CICD_ACCOUNT_ID \
           CMD=$1


    shift; shift;
    ./sso.sh "${CDK_PROFILE}"
    export CDK_NEW_BOOTSTRAP=1
    cdk bootstrap --profile "${CDK_PROFILE}" \
        --cloudformation-execution-policies  'arn:aws:iam::aws:policy/AdministratorAccess' \
        --trust "${CDK_DEPLOY_CICD_ACCOUNT_ID}","${CDK_DEPLOY_INFRASTRUCTURE_ACCOUNT_ID}" \
        || exit

    cdk $CMD --profile "${CDK_PROFILE}" "$@"
    exit $?
else
    echo 1>&2 "Missing required arguments, sample usage for dev system:"
    echo 1>&2 "./cdk.sh deploy dev"
    echo 1>&2 "./cdk.sh deploy dev S3Stack"
    echo 1>&2 "./cdk.sh deploy dev --all"

    echo 1>&2 "./cdk.sh destroy dev"
    echo 1>&2 "./cdk.sh destroy dev S3Stack"
    echo 1>&2 "./cdk.sh destroy dev --all"

    echo 1>&2 "./cdk.sh ls dev"
    echo 1>&2 "./cdk.sh synth dev"
    exit 1
fi
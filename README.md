# idempiere-micro-spring

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/9029f0b6180444969e2ad56f50aba650)](https://app.codacy.com/app/davidpodhola/idempiere-micro-spring?utm_source=github.com&utm_medium=referral&utm_content=iDempiere-micro/idempiere-micro-spring&utm_campaign=Badge_Grade_Dashboard)
[![CircleCI](https://circleci.com/gh/iDempiere-micro/idempiere-micro-spring/tree/master.svg?style=svg)](https://circleci.com/gh/iDempiere-micro/idempiere-micro-spring/tree/master)
[![Maintainability](https://api.codeclimate.com/v1/badges/279819f3dc54d47b941c/maintainability)](https://codeclimate.com/github/iDempiere-micro/idempiere-micro-spring/maintainability)

 THE repository for iDempiere micro - Java + Kotlin Spring Boot back-end compatible with iDempiere 

## Continuous integration

### CircleCI

- can compile and package
- can create an empty iDempiere 5.1 database
- deploy to the staging server http://idempiere-micro-spring.staging-aws.hsharp.software (e.g. http://idempiere-micro-spring.staging-aws.hsharp.software/actuator)
- can start (boot)
- can serve REST API
- integration test JavaScript client in [Frisby.JS](https://www.frisbyjs.com/) can login, token works (who am I and list users)

For more information about the build process, visit the [iDempiere micro Spring Maven build generated site](https://idempiere-micro.github.io/idempiere-micro-spring-site/).

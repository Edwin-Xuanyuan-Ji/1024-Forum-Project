# 1024 Forum

**Table of Contents**

-[Background](#Background)

-[Project Summary](#Project-Summary)

-[Architecture](#Architecture)

## Background

I develop this website project for two reasons:

1. Practice my backend developing skills as well as learn more technologies.

2. It's fun.

## Project Summary

### Key features

  Post, Comment, Send Message
    
  Like, Follow, System Notification
  
  Search, Authority, Monitor 
 
### Core Technologies

  Spring Boot、SSM
   
  Redis、Kafka、ElasticSearch
  
  Spring Security、Quatz、Caffeine
  
### Highlights
  
  The project is based on Spring Boot and SSM framworks. Functions include state management, transaction management and exception handling.
 
  Like and follow functions are implemented by using Redis.
  
  Asynchronous system notification is implemented by using Kafka.
  
  Search in posts function is implemented by using ElasticSearch and the key words are highlighted.
  
  Two level cache is implemented by using Caffeine and Redis. Visit for hot posts is optimized. 

  Use Spring Security to realize authority management for multi-roles and URL-level authorization.
   
  HyperLogLog and Bitmap are applied to get statistics data of UV(user visit) and DAU(daily active user).
  
  Quartz are applied for task scheduling where calculation of scores of posts and removal of trash files on a specified time are implemented.
  
  Actuator is used for monitor of bean, cache, logs, path and etc. Monitor of connection of database is realized by customized endpoint. 

## Architecture

### Program Architecture

![5fb4d65065ed1f1680c7a3149370dd8](https://user-images.githubusercontent.com/81521033/182968805-35a4c837-c644-4c9f-9234-b41deefd168f.jpg)

## Operation and Maintenance Architecture

![image](https://user-images.githubusercontent.com/81521033/182974581-59e6cd1e-51ec-4c1f-b807-0231e440694a.png)

1024 Forum is deployed on a single AWS EC2 and the file server of it is S3 buckets. Since I don't have many servers I didn't set up a server cluster.

But in my view, the ideal situation for a website that QPS > 1000 should be deployed with clusters as the above pictures.

We can have at least two nginx and CDNs for good performance. Also, servers, Redis, Kafka and etc should be in clusters for advantages such as high performance and high usability. 









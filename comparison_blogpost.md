# Summary

Overall, Apache Airflow is the heavier, more feature-dense and battle-tested service of the two, while Versatile Data Kit is more minimal and lightweight in comparison without sacrificing any fundamental capability and while offering a higher level of extensibility.

# Comparison points

* Pricing
* Scheduling
* Simplicity/Ease of use
* Workloads
* Language


# Pricing

Both are free and open-source. Additionally, both Apache Airflow and Versatile Data Kit are licensed under the Apache License Version 2.0.

# Workload Scheduling

Versatile Data Kit schedules workloads through a cron-like interface part of a Data job’s configuration.
Apache Airflow allows you to schedule DAG runs using a cron string, but also using a `datetime.timedelta` object. The first DAG run will trigger according to the minimum start_date value within the DAG, while subsequent runs will be triggered according to the aforementioned schedule.
Additionally, Airflow’s DAG structure allows you to schedule workloads in an interdependent manner, meaning a certain workload will only be executed if a previous workload or set of workloads was executed successfully first.

# Simplicity/Ease of use

Both projects offer a Helm chart which allows users to deploy the respective services to a Kubernetes cluster. However, by default Airflow uses a SQLite database as its storage, which is not recommended for a production deployment. Users must configure and manage another database service, such as an instance of PostgreSQL, and configure Airflow to connect to it.
Airflow offers both a command-line interface, and a web UI for managing workloads. Versatile Data Kit only offers a command-line interface.
Overall, Apache Airflow is the more feature-dense service, while Versatile Data Kit is more minimal, lightweight and overall simpler to use.

# Workloads

Versatile Data Kit allows you to deploy and run workloads consisting of Python files, SQL scripts, or a mixture of both.
Apache Airflow workloads can be composed of many different types of operators which serve a wider pool of functions compared to Versatile Data Kit.
Operators must be written in Python, so technically speaking any workload that Apache Airflow can execute, Versatile Data Kit could as well, however such use cases are not considered idiomatic.

# Language

Apache Airflow is written almost entirely in Python. Versatile Data Kit’s command-line interface is also written in Python, while its Control Service is written in Java.

# Extensibility

Versatile Data Kit employs an extensible architecture which allows users to develop plugins for it which may provide various new functionality such as connecting to different types of databases or ingestion methods, offering CLI commands, and more. Additionally, Versatile Data Kit already has a wide library of officially-supported plugins.
Apache Airflow offers users the capability to write their own providers and executors, which lets users work at their preferred level of abstraction.

# API Comparison

| Requirement            | Airflow                | Versatile Data Kit     |
|------------------------|------------------------|------------------------|
| UI                     | Web UI and command-line interface |


# Reasons to choose Airflow

You prefer established software which has survived the test of time
You value the Airflow DAG as a workload dependency expression
The wider community support appeals to you as troubleshooting any potential problem will be easier

# Reasons to choose VDK

Your use case is too simple to fully benefit from a heavyweight service such as Airflow
You prefer managing your SQL scripts as files directly
You see value in the higher level of extensibility

# Reasons to use both

As of the release of the Airflow provider for Versatile Data Kit, users are able to operate on Data jobs managed in a deployed instance of the VDK Control Service. This will benefit users who prefer Versatile Data Kit overall, but miss the capacity of Airflow to express interdependent workloads. Now users can develop and use DAGs, where each task triggers a Data job execution through a configured connection to a Control Service.
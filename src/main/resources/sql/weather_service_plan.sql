
create table SYSDBA."weather_service_plan"(
                                              "id" bigint not null,
                                              departmentId varchar not null,
                                              "elements" varchar,
                                              GeoIds varchar,
                                              "status" varchar not null,
                                              "type" varchar not null,
                                              planName varchar
);
-- Alter Table Add Identity --
alter table SYSDBA."weather_service_plan" alter column id BIGINT identity(1,1) primary key;
-- Alter Table Add PrimaryKey Constraint --
alter table SYSDBA."weather_service_plan" add constraint pk_weather_service_plan_id primary key(id);
create table SYSDBA."weather_service_geo_detail"(
                                                    staId bigint not null,
                                                    "lon" double not null,
                                                    "lat" double not null,
                                                    geoId bigint not null,
                                                    stationName varchar,
                                                    stationCode varchar
);
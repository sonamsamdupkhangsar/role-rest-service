create table if not exists Role(id uuid primary key, name varchar, user_id uuid);
drop table if exists Client_User_Role;
create table if not exists Client_User_Role(id uuid primary key, client_id uuid, user_id uuid, role_id uuid);
create table if not exists Role_Organization(id uuid primary key, role_id uuid, organization_id uuid);
create table if not exists Role_User(id uuid primary key, role_id uuid, user_id uuid);
create table if not exists Client_Organization_User_Role(id uuid primary key, client_id uuid, organization_id uuid, user_id uuid,  role_id uuid);
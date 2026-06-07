create table if not exists Role(id uuid primary key, name varchar, organization_id uuid);
create table if not exists Client_Organization_User_Role(id uuid primary key, client_id uuid, organization_id uuid, user_id uuid,  role_id uuid);

create table if not exists Authz_Manager_Role( id uuid primary key, name varchar);
create unique index if not exists Authz_Manager_Role_Name_Uidx on Authz_Manager_Role(name);
create table if not exists Authz_Manager_Role_Organization( id uuid primary key, authz_manager_role_id uuid, user_id uuid, organization_id uuid);

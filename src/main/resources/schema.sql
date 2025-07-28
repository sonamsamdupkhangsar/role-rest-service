create table if not exists Role(id uuid primary key, name varchar, user_id uuid);
create table if not exists Client_User_Role(id uuid primary key, client_id uuid, user_id uuid, role_id uuid);
create table if not exists Role_Organization(id uuid primary key, role_id uuid, organization_id uuid);
create table if not exists Role_User(id uuid primary key, role_id uuid, user_id uuid);
create table if not exists Client_Organization_User_Role(id uuid primary key, client_id uuid, organization_id uuid, user_id uuid,  role_id uuid);

create table if not exists Authz_Manager_Role( id uuid primary key, name varchar);
create table if not exists Authz_Manager_Role_User( id uuid primary key, authz_manager_role_id uuid, user_id uuid);
create table if not exists Authz_Manager_Role_Organization( id uuid primary key, authz_manager_role_id uuid, user_id uuid, organization_id uuid, authz_manager_role_user_id UUID);

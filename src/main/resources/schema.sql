create table if not exists Role(id uuid primary key, name varchar);
create table if not exists Role_Client_User(id uuid primary key, client_id varchar, user_id uuid, role_id uuid);
create table if not exists Role_Organization(id uuid primary key, role_id uuid, organization_id uuid);
create table if not exists Role_User(id uuid primary key, role_id uuid, user_id uuid);
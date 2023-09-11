create table if not exists Role(id uuid primary key, organization_id uuid, name varchar);
create table if not exists Role_User(id uuid primary key, client_id uuid, user_id uuid, role_id uuid);
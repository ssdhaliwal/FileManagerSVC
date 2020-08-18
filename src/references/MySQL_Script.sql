drop database if exists filemanagersvc;
create database filemanagersvc;

DELETE FROM mysql.user WHERE user = 'filemanageruser';
grant all privileges on filemanagersvc.* to 'filemanageruser'@'%' identified by 'fmcu';

use filemanagersvc;
drop table if exists fileuser;
create table fileuser (
	id bigint not null auto_increment primary key,
    username varchar(80) not null
);
create unique index uq_fileuser on fileuser(username);

drop table if exists filepath;
create table filepath (
	id bigint not null auto_increment primary key,
    domain varchar(50) not null,
    path varchar(50) not null,
    date_updated TIMESTAMP default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
create unique index uq_filepath on filepath(domain, path);

drop table if exists filelist;
create table filelist (
	id bigint not null auto_increment primary key,
    fileuser_id bigint not null,
    filepath_id bigint not null,
    filename varchar(250) not null,
    ispublic varchar(1) not null default 'N',
    mimetype varchar(35) not null,
    date_updated TIMESTAMP default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
create unique index uq_filelist on filelist(fileuser_id, filename);
create index ix_filelist_file on filelist(filename);

drop table if exists filedata;
create table filedata (
	id bigint not null auto_increment primary key,
    filelist_id bigint not null,
    data longblob null not null
);
create unique index uq_filedata on filedata(filelist_id);

delimiter //
drop procedure if exists updateFile //
create procedure updateFile(IN iusername varchar(80), IN ifilename varchar(250), IN iispublic varchar(1),
	IN imimetype varchar(35), IN idata longblob, IN idomain varchar(50), IN ipath varchar(50),
	OUT ouserid bigint, OUT opathid bigint, OUT ofileid bigint, OUT odataid bigint)
begin
	declare user_id bigint default 0;
    declare path_id bigint default 0;
    declare file_id bigint default 0;
    declare data_id bigint default 0;
    
    -- exception handling
    -- declare continue handler for sqlwarning
    -- begin
    -- end;
    declare continue handler for sqlexception
    begin
		rollback to savepoint user_created;
        release savepoint user_created;
    end;
    -- declare continue handler for not found
    -- begin
    -- end;

    start transaction WITH CONSISTENT SNAPSHOT;
        
    -- check and update user
    select id into user_id from fileuser where username = LOWER(iusername);
    if (user_id = 0) then
		insert into fileuser(username) values (LOWER(iusername));
        select LAST_INSERT_ID() into user_id;
    end if;
    
    savepoint user_created;

	-- check the file path
    select id into path_id from filepath where domain = LOWER(idomain) and path = LOWER(ipath);
    if (path_id = 0) then
		insert into filepath(domain, path) values (LOWER(idomain), LOWER(ipath));
        select LAST_INSERT_ID() into path_id;
    end if;
    
	-- check and update filelist
	select id into file_id from filelist where fileuser_id = user_id and filename = ifilename;

    -- if data is not null, add/update
    if (idata IS NOT null) then
		if (file_id = 0) then
			insert into filelist(fileuser_id, filename, ispublic, mimetype, filepath_id) values
				(user_id, ifilename, iispublic, imimetype, path_id);
			select LAST_INSERT_ID() into file_id;
		end if;
		
		-- check and store the filedata (delete old first)
		delete from filedata where filelist_id = file_id;
		insert into filedata (filelist_id, data) values (file_id, idata);
		select LAST_INSERT_ID() into data_id;
	else
		-- delete all info for the file
        delete from filelist where fileuser_id = user_id and filename = ifilename;
		delete from filedata where filelist_id = file_id;
    end if;
    
    -- cleanup transcation
	release savepoint user_created;
	commit;

    -- return variables
    set ouserid = user_id;
    set opathid = path_id;
    set ofileid = file_id;
    set odataid = data_id;
end //

drop view if exists vwFile //
create view vwFile as
select a.id as user_id, a.username, d.domain, d.path,
	b.id as file_id, b.filename, b.ispublic, b.mimetype, b.date_updated
    from fileuser a
    inner join filelist b on b.fileuser_id = a.id
    inner join filepath d on d.id = b.filepath_id //

drop view if exists vwFileData //
create view vwFileData as
select a.id as user_id, a.username, d.domain, d.path, 
	b.id as file_id, b.filename, b.ispublic, b.mimetype, b.date_updated,
	c.data
    from fileuser a
	inner join filelist b on b.fileuser_id = a.id
    inner join filepath d on d.id = b.filepath_id
    inner join filedata c on c.filelist_id = b.id //
delimiter ;

select * from vwFile;
select * from vwFileData;
select 'filelist' as name, count(*) from filelist union select 'filedata' as name, count(*) from filedata;

call updateFile("test", "test", "Y", "image/png", "tehis is a test", "mil.uscg.cg1v", "catalog", @oid, @pid, @fid, @did);


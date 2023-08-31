package com.publicis.sapient.p2p.repository;

import com.publicis.sapient.p2p.model.ImageDump;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ImageDumpRepository extends MongoRepository<ImageDump, String> {
    @Query(value = "{ 'url' : { $in: ?0 } }", delete = true)
    void deleteAllByUrl(List<String> urls);
}

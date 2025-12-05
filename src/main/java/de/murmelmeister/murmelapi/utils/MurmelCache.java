package de.murmelmeister.murmelapi.utils;

import de.murmelmeister.murmelapi.utils.update.RefreshListener;

public interface MurmelCache extends RefreshListener {
    void close();
}

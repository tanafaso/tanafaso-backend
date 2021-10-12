package com.azkar.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class QuranService {

  public int getFirstAyahInJuz(int juz) {
    return 0;
  }


  public int getLastAyahInJuz(int juz) {

    return 0;
  }

  // TODO: don't return the first ayahs of the Quran or the last ayahs
  public int getRandomAyahInJuz(int juz) {

    return 0;
  }

  public int getRandomJuzInRange(int firstJuzInclusive, int lastJuzInclusive) {
    Random random = new Random();
    return random.nextInt(lastJuzInclusive - firstJuzInclusive + 1) + firstJuzInclusive;
  }

  public List<Integer> getRandomTwoWrongPreviousAyahs(int ayah) {
    return new ArrayList<>();
  }

  public List<Integer> getRandomTwoWrongNextAyahs(int ayah) {

    return new ArrayList<>();
  }

  public List<Integer> getRandomTwoWrongFirstAyahsInRub(int rub) {

    return new ArrayList<>();
  }

  public List<Integer> getRandomTwoWrongFirstAyahsInJuz(int juz) {

    return new ArrayList<>();
  }

  public List<Integer> getRandomTwoWrongSurahsOfAyah(int ayah) {

    return new ArrayList<>();
  }

  public int getRubOfAya(int ayah) {
    return 0;
  }

  public int getFirstAyahInRub(int rub) {
    return 0;
  }

  public int getSurahOfAyah(int ayah) {
    return 0;
  }
}

package fyi.meld.presto.models

import android.util.Log
import androidx.databinding.Observable
import fyi.meld.presto.utils.Constants

class StoreTrip ()
{
    var items = arrayListOf<CartItem>()
    var localTaxRate : Float = 8.26f;

    private var runningTotal : Float = 0.00f;

    var totalAfterTax : Float = 0.00f
        private set
        get() {
            return (runningTotal * (localTaxRate / 100f)) + runningTotal
        }

    var size : Int = 0
        private set
        get() {
            var s = 0

            for(i in items)
            {
                s += i.qty
            }

            return s
        }

    fun addToCart(item : CartItem)
    {
        items.add(item)
        updateTotals()
    }

    fun removeFromCart(item : CartItem)
    {
        items.remove(item)
        updateTotals()
    }

    public fun updateTotals()
    {
        runningTotal = 0.00f;
        for(i in items)
        {
            runningTotal += i.getPrice()
        }
    }

}